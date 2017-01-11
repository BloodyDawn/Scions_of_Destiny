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
public class MoveToLocationInVehicle extends ServerBasePacket
{
    private int _pciId;
    private int _boatId;
    private Point3D _destination;
    private Point3D _origin;

    /**
     * @param player
     * @param destination
     * @param origin
     */
    public MoveToLocationInVehicle(L2PcInstance player, Point3D destination, Point3D origin)
    {
        _pciId = player.getObjectId();
        _boatId = player.getBoat().getObjectId();
        _destination = destination;
        _origin = origin;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    final void writeImpl()
    {
        writeC(0x71);
        writeD(_pciId);
        writeD(_boatId);
        writeD(_destination.getX());
        writeD(_destination.getY());
        writeD(_destination.getZ());
        writeD(_origin.getX());
        writeD(_origin.getY());
        writeD(_origin.getZ());		
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] 71 MoveToLocationInVehicle";
    }
}