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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Point3D;

/**
 * @author Maktakien
 *
 */
public class StopMoveInVehicle extends ServerBasePacket
{
    private int _charObjId;
    private int _boatId;
    private Point3D _pos;
    private int _heading;

    public StopMoveInVehicle(L2PcInstance player, int boatId)
    {
        _charObjId = player.getObjectId();
        _boatId = boatId;
        _pos = player.getInBoatPosition();
        _heading = player.getHeading();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    final void writeImpl()
    {		
        writeC(0x72);
        writeD(_charObjId);
        writeD(_boatId);
        writeD(_pos.getX());
        writeD(_pos.getY());
        writeD(_pos.getZ());
        writeD(_heading);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        // TODO Auto-generated method stub
        return "[S] 72 StopMoveInVehicle";
    }
}