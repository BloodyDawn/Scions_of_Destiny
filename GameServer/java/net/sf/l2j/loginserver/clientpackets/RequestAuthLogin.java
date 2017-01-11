/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.loginserver.clientpackets;

import java.security.GeneralSecurityException;

import java.util.List;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.ClientThread;
import net.sf.l2j.loginserver.ClientThread.LoginClientState;
import net.sf.l2j.loginserver.GameServerListener;
import net.sf.l2j.loginserver.GameServerTable;
import net.sf.l2j.loginserver.GameServerThread;
import net.sf.l2j.loginserver.HackingException;
import net.sf.l2j.loginserver.LoginController;
import net.sf.l2j.loginserver.serverpackets.AccountKicked;
import net.sf.l2j.loginserver.serverpackets.LoginFail;
import net.sf.l2j.loginserver.serverpackets.LoginOk;

/**
 * Format: x
 * 0 (a leading null)
 * x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends ClientBasePacket
{
    private static Logger _log = Logger.getLogger(RequestAuthLogin.class.getName());

    private String _account;
    private String _password;
    
    public RequestAuthLogin(byte[] rawPacket, ClientThread client)
    {
        super(rawPacket, client);
    }

    @Override
    public void run()
    {
        try
        {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getPrivateKey());
            byte[] decrypted = rsaCipher.doFinal(getByteBuffer(), 0x01, 0x80);

            _account = new String(decrypted, 0x62, 14).trim();
            _account = _account.toLowerCase();
            _password = new String(decrypted, 0x70, 16).trim();
        }
        catch (GeneralSecurityException e)
        {
            e.printStackTrace();
            return;
        }
        catch (IllegalArgumentException e)
        {
            // system folder is corrupted
            _log.warning("Account with IP "+getClient().getSocket().getInetAddress().getHostAddress()+" is attempting to login with corrupted system.");
            return;
        }        

        LoginController login = LoginController.getInstance();

        // ip BANNED due to entering wrong password many times
        if (login.isBannedAddress(getClient().getSocket().getInetAddress().getHostAddress()))
        {
            getClient().sendPacket(new AccountKicked(AccountKicked.REASON_ILLEGAL_USE));
            return;
        }

        try
        {
            if (!login.isLoginValid(_account, _password, getClient()))
            {
                getClient().sendPacket(new LoginFail(LoginFail.REASON_USER_OR_PASS_WRONG));
                return;
            }
        }
        catch (HackingException e)
        {
            _log.warning("Hacking Exception by account "+_account);
            return;
        }

        // Account BANNED (must always be checked after isLoginValid)
        if (getClient().getAccessLevel() < 0)
        {
            getClient().sendPacket(new AccountKicked(AccountKicked.REASON_ILLEGAL_USE));
            return;
        }

        getClient().setAccount(_account);

        ClientThread connected = login.getConnectedClient(_account);
        if (connected != null)
        {
            connected.sendPacket(new LoginFail(LoginFail.REASON_ACCOUNT_IN_USE));
            getClient().sendPacket(new LoginFail(LoginFail.REASON_ACCOUNT_IN_USE));
            return;
        }

        List<GameServerThread> gslist = GameServerListener.getInstance().getGameServerThreads();
        synchronized (gslist)
        {
            for (GameServerThread gameServer : gslist)
            {
                if (gameServer.getPlayersInGame().contains(_account))
                {
                    gameServer.kickPlayer(_account);
                    getClient().sendPacket(new LoginFail(LoginFail.REASON_ACCOUNT_IN_USE));
                    return;
                }
            }
        }

        getClient().setState(LoginClientState.AUTHED_LOGIN);
        login.assignKeyToLogin(getClient());

        if (Config.SHOW_LICENCE)
            getClient().sendPacket(new LoginOk(getClient().getSessionKey()));
        else
            GameServerTable.getInstance().createServerList(getClient());
    }
}