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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.PartyMatchRoom;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Gnacik
 */
public class ExPartyRoomMember extends ServerBasePacket
{
    private final PartyMatchRoom _room;
    private final int _mode;

    public ExPartyRoomMember(L2PcInstance player, PartyMatchRoom room, int mode)
    {
        _room = room;
        _mode = mode;
    }

    final void writeImpl()
    {
        writeC(0xfe);
        writeH(0x0e);
        writeD(_mode);
        writeD(_room.getMembers());
        for (L2PcInstance _member : _room.getPartyMembers())
        {
            writeD(_member.getObjectId());
            writeS(_member.getName());
            writeD(_member.getActiveClass());
            writeD(_member.getLevel());
            writeD(MapRegionTable.getInstance().getClosestTownNumber(_member));
            if (_room.getOwner().equals(_member))
                writeD(1);
            else
            {
                if ((_room.getOwner().isInParty() && _member.isInParty()) && (_room.getOwner().getParty().getPartyLeaderOID() == _member.getParty().getPartyLeaderOID()))
                    writeD(2);
                else
                    writeD(0);
            }
        }
    }

    public String getType()
    {
        return "[S] FE:0E ExPartyRoomMember";
    }
}