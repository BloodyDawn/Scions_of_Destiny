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
package net.sf.l2j.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.FriendRecvMsg;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Recieve Private (Friend) Message - 0xCC
 * 
 * Format: c SS
 * 
 * S: Message
 * S: Receiving Player
 * 
 * @author Tempy
 * 
 */
public class RequestSendFriendMsg extends ClientBasePacket
{
    private static final String _C__CC_REQUESTSENDMSG = "[C] CC RequestSendMsg";
    private static Logger _logChat = Logger.getLogger(RequestSendFriendMsg.class.getName());

    private String _message;
    private String _receiver;

    public RequestSendFriendMsg(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _message = readS();
        _receiver = readS();
    }

    @Override
    public void runImpl()
    {
    	L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        if (_message == null || _message.isEmpty() || _message.length() > 300)
            return;

        if (activeChar.getFriend(_receiver) == null)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_NOT_FOUND_IN_THE_GAME));
            return;
        }

        L2PcInstance targetPlayer = L2World.getInstance().getPlayer(_receiver);
        if (targetPlayer == null) 
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_NOT_FOUND_IN_THE_GAME));
            return;
        }

        if (Config.LOG_CHAT) 
        {
            LogRecord record = new LogRecord(Level.INFO, _message);
            record.setLoggerName("chat");
            record.setParameters(new Object[]{"PRIV_MSG", "[" + activeChar.getName() + " to "+ _receiver +"]"});
            _logChat.log(record);
        }

        FriendRecvMsg frm = new FriendRecvMsg(activeChar.getName(), _receiver, _message);
        targetPlayer.sendPacket(frm);
    }

    public String getType()
    {
        return _C__CC_REQUESTSENDMSG;
    }
}