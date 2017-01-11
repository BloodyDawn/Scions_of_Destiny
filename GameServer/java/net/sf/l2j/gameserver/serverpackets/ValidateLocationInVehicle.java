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
 * This class ...
 * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ValidateLocationInVehicle extends ServerBasePacket
{
    private static final String _S__73_ValidateLocationInVehicle = "[S] 73 ValidateLocationInVehicle";

    private int _charObjId;
    private int _boatObjId;
    private int _heading;
    private Point3D _pos;

    /**
     * 0x73 ValidateLocationInVehicle         hdd 
     * @param _characters
     */
    public ValidateLocationInVehicle(L2PcInstance player)
    {
        _charObjId = player.getObjectId();
        _boatObjId = player.getBoat().getObjectId();
        _heading = player.getHeading();
        _pos = player.getInBoatPosition();
    }

    final void writeImpl()
    {
        writeC(0x73);
        writeD(_charObjId);
        writeD(_boatObjId);
        writeD(_pos.getX());
        writeD(_pos.getY());
        writeD(_pos.getZ());
        writeD(_heading);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__73_ValidateLocationInVehicle;
    }
}