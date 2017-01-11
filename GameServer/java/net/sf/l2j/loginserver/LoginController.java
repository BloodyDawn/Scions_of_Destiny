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
package net.sf.l2j.loginserver;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.util.Rnd;

/**
 * This class ...
 * 
 * @version $Revision: 1.7.4.3 $ $Date: 2005/03/27 15:30:09 $
 */
public class LoginController
{
    protected static Logger _log = Logger.getLogger(LoginController.class.getName());

    private static LoginController _instance;

    /** this map contains the connections of the players that are in the loginserver*/
    private FastMap<String, ClientThread> _accountsInLogin;
    private Map<String, BanInfo> _bannedIps;
    private int _maxAllowedOnlinePlayers;
    private Map<String, Integer> _hackProtection;
    private Map<String, String> _lastPassword;
    protected KeyPairGenerator _keyGen;
    protected ScrambledKeyPair[] _keyPairs;
    private AtomicInteger _keyPairToUpdate;
    private long _lastKeyPairUpdate;

    /**
     * <p>This class is used to represent session keys used by the client to authenticate in the gameserver</p>
     * <p>A SessionKey is made up of two 8 bytes keys. One is send in the {@link net.sf.l2j.loginserver.serverpacket.LoginOk LoginOk}
     * packet and the other is sent in {@link net.sf.l2j.loginserver.serverpacket.PlayOk PlayOk}</p>
     * @author -Wooden-
     *
     */
    public static class SessionKey
    {
        public int playOkID1;
        public int playOkID2;
        public int loginOkID1;
        public int loginOkID2;

        public SessionKey(int playOK1, int loginOK2, int loginOK1, int playOK2)
        {
            playOkID1 = playOK1;
            playOkID2 = playOK2;
            loginOkID1 = loginOK1;
            loginOkID2 = loginOK2;
        }

        public boolean checkLoginPair(int loginOk1, int loginOk2)
	{
            return loginOkID1 == loginOk1 && loginOkID2 == loginOk2;
	}

        /**
         * <p>Returns true if keys are equal.</p>
         * <p>Only checks the PlayOk part of the session key if server doesnt show the licence when player logs in.</p>
         * @param key
         */
        public boolean equals(SessionKey key)
        {
            // when server doesnt show licence it doesnt send the LoginOk packet, client doesnt have this part of the key then.
            if (Config.SHOW_LICENCE)
                return ((playOkID1 == key.playOkID1 || loginOkID1 == key.playOkID1) && (loginOkID1 == key.loginOkID1
                || playOkID1 == key.loginOkID1) && playOkID2 == key.playOkID2 && loginOkID2 == key.loginOkID2);
            else
                return ((playOkID1 == key.playOkID1 || playOkID1 == key.loginOkID1) && playOkID2 == key.playOkID2);
        }
    }

    private LoginController()
    {
        _log.info("Initializing LoginController");
        _accountsInLogin = new FastMap<String, ClientThread>().shared();
        _bannedIps = new FastMap<String, BanInfo>().shared();
        _hackProtection = new FastMap<>();
        _lastPassword = new FastMap<>();
        _keyPairToUpdate = new AtomicInteger(0);
        _keyPairs = new ScrambledKeyPair[10];

        try
        {
            _keyGen = KeyPairGenerator.getInstance("RSA");
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
            _keyGen.initialize(spec);
        }
        catch (GeneralSecurityException e)
        {
            _log.severe("FATAL: Failed initializing LoginController. Reason: "+e.getMessage());
            if (Config.DEVELOPER)
                e.printStackTrace();
            System.exit(1);
        }

        if (Config.DEBUG)
            _log.info("LoginController : RSA keygen initiated");

        // generate the initial set of keys
        for (int i = 0; i < 10; i++)
            _keyPairs[i] = new ScrambledKeyPair(_keyGen.generateKeyPair());

        _lastKeyPairUpdate = System.currentTimeMillis();
        _log.info("Stored 10 KeyPair for RSA communication");
    }

    public static LoginController getInstance()
    {
        if (_instance == null)
            _instance = new LoginController();

        return _instance;
    }

    public void assignKeyToLogin(ClientThread client)
    {
        client.setSessionKey(new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt()));
        _accountsInLogin.put(client.getAccount(), client);
    }

    public SessionKey getKeyForAccount(String account)
    {
        if (_accountsInLogin.get(account) == null)
            return null;

        return _accountsInLogin.get(account).getSessionKey();
    }

    public void removeLoginClient(String account)
    {
        if (account != null)
            _accountsInLogin.remove(account);
    }

    public ClientThread getConnectedClient(String account)
    {
        if (account == null)
            return null;

        return _accountsInLogin.get(account);
    }

    public int getTotalOnlinePlayerCount()
    {
        int playerCount = 0;
        List<GameServerThread> gslist = GameServerListener.getInstance().getGameServerThreads();
        synchronized (gslist)
        {
            for (GameServerThread gs : gslist)
            {
                playerCount += gs.getCurrentPlayers();
            }
        }
        return playerCount;
    }

    public int getOnlinePlayerCount(int ServerID)
    {
        List<GameServerThread> gslist = GameServerListener.getInstance().getGameServerThreads();
        synchronized (gslist)
        {
            for (GameServerThread gs : gslist)
            {
                if (gs.getServerID() == ServerID)
                    return gs.getCurrentPlayers();
            }
        }
        return 0;
    }

    public int getMaxAllowedOnlinePlayers(int ServerID)
    {
        List<GameServerThread> gslist = GameServerListener.getInstance().getGameServerThreads();
        synchronized (gslist)
        {
            for (GameServerThread gs : gslist)
            {
                if (gs.getServerID() == ServerID)
                    return gs.getMaxPlayers();
            }
        }
        return 0;
    }

    public void setMaxAllowedOnlinePlayers(int maxAllowedOnlinePlayers)
    {
        _maxAllowedOnlinePlayers = maxAllowedOnlinePlayers;
    }

    /**
     * @return
     */
    public boolean loginPossible(int access, int ServerID)
    {
        return ((getOnlinePlayerCount(ServerID) < _maxAllowedOnlinePlayers) || (access >= 50));
    }

    public void setAccountAccessLevel(String account, int banLevel)
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?"))
        {
            statement.setInt(1, banLevel);
            statement.setString(2, account);
            statement.executeUpdate();
        }
        catch (Exception e)
        {
            _log.warning("Could not set accessLevel:" + e);
        }
    }

    /**
     * <p>This method returns one of the 10 {@link ScrambledKeyPair}.</p>
     * <p>One of them the re-newed asynchronously using a {@link UpdateKeyPairTask} if necessary.</p>
     * @return a scrambled keypair
     */
    public ScrambledKeyPair getScrambledRSAKeyPair()
    {
        // ensure that the task will update the keypair only after a keypair is returned.
        synchronized (_keyPairs)
        {
            if ((System.currentTimeMillis() - _lastKeyPairUpdate) > 1200000) // update a key every 20 minutes
            {
                if (_keyPairToUpdate.get() == 10) _keyPairToUpdate.set(0);
                UpdateKeyPairTask task = new UpdateKeyPairTask(_keyPairToUpdate.getAndIncrement());
                task.start();
                _lastKeyPairUpdate = System.currentTimeMillis();
            }
        }

        return _keyPairs[Rnd.nextInt(10)];
    }

    public void saveLastServer(String account, int serverId)
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastServer = ? WHERE login = ?"))
        {
            statement.setInt(1, serverId);
            statement.setString(2, account);
            statement.executeUpdate();
        }
        catch (Exception e)
        {
            _log.warning("Could not set lastServer: "+e);
        }
    }

    /**
     * user name is not case sensitive any more
     * @param user
     * @param password
     * @param address
     * @return
     */
    public boolean isLoginValid(String user, String password, ClientThread client) throws HackingException
    {
        boolean ok = false;
        InetAddress address = client.getSocket().getInetAddress();

        Log.add("'" + (user == null ? "null" : user) + "' " + (address == null ? "null" : address.getHostAddress()), "logins_ip");

        if (address == null)
            return false;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection())
        {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] raw = password.getBytes("UTF-8");
            byte[] hash = md.digest(raw);

            byte[] expected = null;
            int access = 0;
            int lastServer = 1;

            try (PreparedStatement statement = con.prepareStatement("SELECT password, access_level, lastServer FROM accounts WHERE login=?"))
            {
                statement.setString(1, user);
                try (ResultSet rset = statement.executeQuery())
                {
                    if (rset.next())
                    {
                        expected = Base64.getDecoder().decode(rset.getString("password"));
                        access = rset.getInt("access_level");
                        lastServer = rset.getInt("lastServer");

                        if (Config.DEBUG)
                            _log.fine("account exists!");
                    }
                }
            }

            if (expected == null)
            {
                if (Config.AUTO_CREATE_ACCOUNTS)
                {
                    if (user.length() >= 2 && user.length() <= 14)
                    {
                        try (PreparedStatement statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,lastIP) values(?,?,?,?,?)"))
                        {
                            statement.setString(1, user);
                            statement.setString(2, Base64.getEncoder().encodeToString(hash));
                            statement.setLong(3, System.currentTimeMillis());
                            statement.setInt(4, 0);
                            statement.setString(5, address.getHostAddress());
                            statement.execute();
                        }

                        _log.info("Created new account for " + user);
                        return true;

                    }
                    _log.warning("Invalid username creation/use attempt: " + user);
                    return false;
                }
                _log.warning("["+address.getHostAddress()+"]: account missing for user " + user);
                return false;
            }

            ok = true;
            for (int i = 0; i < expected.length; i++)
            {
                if (hash[i] != expected[i])
                {
                    ok = false;
                    break;
                }
            }
            if (ok)
            {
                client.setAccessLevel(access);
                client.setLastServer(lastServer);
                try (PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastactive=?, lastIP=? WHERE login=?"))
                {
                    statement.setLong(1, System.currentTimeMillis());
                    statement.setString(2, address.getHostAddress());
                    statement.setString(3, user);
                    statement.execute();
                }
            }
        }
        catch (Exception e)
        {
            // digest algo not found ??
            // out of bounds should not be possible
            _log.warning("could not check password:" + e);
            ok = false;
        }

        if (!ok)
        {
            Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip_fails");

            Integer failedConnects = _hackProtection.get(address.getHostAddress());
            String lastPassword = _lastPassword.get(address.getHostAddress());

            // add 1 to the failed counter for this IP 
            int failedCount = 1;
            if (failedConnects != null)
                failedCount = failedConnects.intValue() + 1;

            if (password != lastPassword)
            {
                _hackProtection.put(address.getHostAddress(), new Integer(failedCount));
                _lastPassword.put(address.getHostAddress(), password);
            }

            if (failedCount >= Config.LOGIN_TRY_BEFORE_BAN)
            {
                _log.info("Banning '"+address.getHostAddress()+"' for "+Config.LOGIN_BLOCK_AFTER_BAN+" seconds due to "+failedCount+" invalid user/pass attempts");
                addBannedIP(address.getHostAddress(), System.currentTimeMillis() + (Config.LOGIN_BLOCK_AFTER_BAN*1000));
            }
        }
        else
        {
            // for long running servers, this should prevent blocking 
            // of users that mistype their passwords once every day :)
            _hackProtection.remove(address.getHostAddress());
            _lastPassword.remove(address.getHostAddress());
            Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip");
        }

        return ok;
    }

    public boolean ipBlocked(String ipAddress)
    {
        int tries = 0;

        if (_hackProtection.containsKey(ipAddress)) tries = _hackProtection.get(ipAddress);

        if (tries > Config.LOGIN_TRY_BEFORE_BAN)
        {
            _hackProtection.remove(ipAddress);
            _log.warning("Removed host from hacklist! IP number: " + ipAddress);
            return true;
        }

        return false;
    }

    public void addBannedIP(String address, long expiration)
    {
        _bannedIps.put(address, new BanInfo(expiration));
    }

    public boolean isBannedAddress(String address)
    {
        BanInfo ban = _bannedIps.get(address);
        if (ban != null)
        {
            if (!ban.hasExpired())
                return true;
            _bannedIps.remove(address);
        }
        return false;
    }

    public Map<String, BanInfo> getBannedIps()
    {
        return _bannedIps;
    }

    private class BanInfo
    {
        private long _time;
        public BanInfo(long time)
        {
            _time = time;
        }

        public boolean hasExpired()
        {
            return System.currentTimeMillis() > _time && _time > 0;
        }
    }

    private class UpdateKeyPairTask extends Thread
    {
        private int _keyPairId;

        public UpdateKeyPairTask(int keyPairId)
        {
            _keyPairId = keyPairId;
        }

        public void run()
        {
            _keyPairs[_keyPairId] = new ScrambledKeyPair(_keyGen.generateKeyPair());

            if (Config.DEBUG) _log.info("Updated a RSA key");
        }
    }

    public static class ScrambledKeyPair
    {
        public KeyPair pair;
        public byte[] scrambledModulus;

        public ScrambledKeyPair(KeyPair pPair)
        {
            this.pair = pPair;
            scrambledModulus = scrambleModulus(((RSAPublicKey) this.pair.getPublic()).getModulus());
        }

        private byte[] scrambleModulus(BigInteger modulus)
        {
            byte[] scrambledMod = modulus.toByteArray();

            if (scrambledMod.length == 0x81 && scrambledMod[0] == 0x00)
            {
                byte[] temp = new byte[0x80];
                System.arraycopy(scrambledMod, 1, temp, 0, 0x80);
                scrambledMod = temp;
            }
            // step 1 : 0x4d-0x50 <-> 0x00-0x04
            for (int i = 0; i < 4; i++)
            {
                byte temp = scrambledMod[0x00 + i];
                scrambledMod[0x00 + i] = scrambledMod[0x4d + i];
                scrambledMod[0x4d + i] = temp;
            }
            // step 2 : xor first 0x40 bytes with  last 0x40 bytes
            for (int i = 0; i < 0x40; i++)
            {
                scrambledMod[i] = (byte) (scrambledMod[i] ^ scrambledMod[0x40 + i]);
            }
            // step 3 : xor bytes 0x0d-0x10 with bytes 0x34-0x38
            for (int i = 0; i < 4; i++)
            {
                scrambledMod[0x0d + i] = (byte) (scrambledMod[0x0d + i] ^ scrambledMod[0x34 + i]);
            }
            // step 4 : xor last 0x40 bytes with  first 0x40 bytes
            for (int i = 0; i < 0x40; i++)
            {
                scrambledMod[0x40 + i] = (byte) (scrambledMod[0x40 + i] ^ scrambledMod[i]);
            }
            _log.fine("Modulus was scrambled");

            return scrambledMod;
        }
    }
}