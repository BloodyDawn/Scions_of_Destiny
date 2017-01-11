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
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.PartyMatchRoom;
import net.sf.l2j.gameserver.model.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.serverpackets.PartyMatchDetail;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Packetformat  Rev650  cdddddS
 * 
 * @version $Revision: 1.1.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPartyMatchList extends ClientBasePacket
{
    private static final String _C__70_REQUESTPARTYMATCHLIST = "[C] 70 RequestPartyMatchList";
    private static Logger _log = Logger.getLogger(RequestPartyMatchList.class.getName());

    private int _roomid;
    private int _membersmax;
    private int _lvlmin;
    private int _lvlmax;
    private int _loot;
    private String _roomtitle;

    /**
     * packet type id 0x70
     * 
     * sample
     * 
     * 70
     * 01 00 00 00 
     * 
     * format:		cd 
     * @param decrypt
     */
    public RequestPartyMatchList(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _roomid = readD();
        _membersmax = readD();
        _lvlmin = readD();
        _lvlmax = readD();
        _loot = readD();
        _roomtitle = readS();
    }

    @Override
    public void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        if (activeChar.getParty() != null && activeChar.getParty().getPartyMembers().get(0) != activeChar)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANT_ENTER_PARTY_ROOM));
            return;
        }

        if (_roomid > 0)
        {
            PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);
            if (_room != null)
            {
                if (Config.DEBUG)
                    _log.info("PartyMatchRoom #" + _room.getId() + " changed by "+activeChar.getName());

                _room.setMaxMembers(_membersmax);
                _room.setMinLvl(_lvlmin);
                _room.setMaxLvl(_lvlmax);
                _room.setLootType(_loot);
                _room.setTitle(_roomtitle);

                for (L2PcInstance _member : _room.getPartyMembers())
                {
                    if (_member == null)
                        continue;

                    _member.sendPacket(new PartyMatchDetail(_room));
                    _member.sendPacket(new SystemMessage(SystemMessage.PARTY_ROOM_REVISED));
                }
            }
        }
        else
        {
            int _newId = PartyMatchRoomList.getInstance().getAutoIncrementId();
            PartyMatchRoom _room = new PartyMatchRoom(_newId, _roomtitle, _loot, _lvlmin, _lvlmax, _membersmax, activeChar);

            if (Config.DEBUG)
                _log.info("PartyMatchRoom #" + _newId + " created by " + activeChar.getName());

            PartyMatchRoomList.getInstance().addPartyMatchRoom(_room);

            if (activeChar.isInParty())
            {
                for (L2PcInstance ptmember : activeChar.getParty().getPartyMembers())
                {
                    if (ptmember == null)
                        continue;

                    if (ptmember == activeChar)
                        continue;

                    ptmember.setPartyRoom(_newId);

                    _room.addMember(ptmember);
                }
            }

            activeChar.sendPacket(new PartyMatchDetail(_room));
            activeChar.sendPacket(new ExPartyRoomMember(activeChar, _room, 1));

            activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_ROOM_CREATED));

            activeChar.setPartyRoom(_newId);
            activeChar.broadcastUserInfo();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__70_REQUESTPARTYMATCHLIST;
    }
}