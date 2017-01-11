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
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.PartyMatchRoom;
import net.sf.l2j.gameserver.model.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ExClosePartyRoom;
import net.sf.l2j.gameserver.serverpackets.PartyMatchList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * format (ch) d
 * @author -Wooden-
 *
 */
public class RequestOustFromPartyRoom extends ClientBasePacket
{
    private static final String _C__D0_01_REQUESTOUSTFROMPARTYROOM = "[C] D0:01 RequestOustFromPartyRoom";

    private int _charId;

    /**
     * @param buf
     * @param client
     */
    public RequestOustFromPartyRoom(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _charId = readD();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    public void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        L2Object object = L2World.getInstance().findObject(_charId);
        if (object == null)
            return;

        if (!(object instanceof L2PcInstance))
            return;

        L2PcInstance member = (L2PcInstance)object;

        PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(member);
        if (_room == null)
            return;

        if (_room.getOwner() != activeChar)
            return;

        if (_room.getOwner() == member)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.INCORRECT_TARGET));
            return;
        }

        if (activeChar.isInParty() && member.isInParty() && activeChar.getParty().getPartyLeaderOID() == member.getParty().getPartyLeaderOID())
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISMISS_PARTY_MEMBER));
        else
        {
            // Remove member from party room
            _room.deleteMember(member);
            member.setPartyRoom(0);

            // Close the PartyRoom window
            member.sendPacket(new ExClosePartyRoom());

            // Send Room list
            int loc = MapRegionTable.getInstance().getClosestTownNumber(member);
            member.sendPacket(new PartyMatchList(member, 0, loc, member.getLevel()));

            // Clean Looking for Party title
            member.broadcastUserInfo();
            member.sendPacket(new SystemMessage(SystemMessage.OUSTED_FROM_PARTY_ROOM));
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_01_REQUESTOUSTFROMPARTYROOM;
    }
}