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

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.PartyMatchRoom;
import net.sf.l2j.gameserver.model.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.serverpackets.PartyMatchDetail;
import net.sf.l2j.gameserver.serverpackets.PartyMatchList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPartyMatchConfig extends ClientBasePacket
{
    private static final String _C__6F_REQUESTPARTYMATCHCONFIG = "[C] 6F RequestPartyMatchConfig";

    private int _auto, _loc, _lvl;

    /**
     * packet type id 0x6f
     * 
     * sample
     * 
     * 6f
     * 01 00 00 00 
     * 00 00 00 00 
     * 00 00 00 00 
     * 00 00 
     * 
     * format:		cdddS 
     * @param decrypt
     */
    public RequestPartyMatchConfig(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _auto = readD();
        _loc = readD(); // Location
        _lvl = readD(); // my level
    }

    @Override
    public void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        if (!activeChar.isInPartyMatchRoom() && activeChar.getParty() != null && activeChar.getParty().getPartyMembers().get(0) != activeChar)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANT_VIEW_PARTY_ROOMS));
            activeChar.sendPacket(new ActionFailed());
            return;
        }

        if (activeChar.isInPartyMatchRoom())
        {
            // If Player is in Room show him room, not list
            PartyMatchRoomList _list = PartyMatchRoomList.getInstance();
            if (_list == null)
                return;

            PartyMatchRoom _room = _list.getPlayerRoom(activeChar);
            if (_room == null)
                return;

            activeChar.sendPacket(new PartyMatchDetail(_room));

            if (activeChar == _room.getOwner())
                activeChar.sendPacket(new ExPartyRoomMember(activeChar, _room, 1));
            else
                activeChar.sendPacket(new ExPartyRoomMember(activeChar, _room, 2));

            activeChar.setPartyRoom(_room.getId());
            activeChar.broadcastUserInfo();
        }
        else
        {
            // Send Room list
            PartyMatchList matchList = new PartyMatchList(activeChar,_auto,_loc,_lvl);

            activeChar.sendPacket(matchList);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__6F_REQUESTPARTYMATCHCONFIG;
    }
}