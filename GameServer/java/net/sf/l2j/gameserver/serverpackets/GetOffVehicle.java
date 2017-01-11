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

/**
 * @author Maktakien
 *
 */
public class GetOffVehicle extends ServerBasePacket
{
    private int _charObjId, _boatObjId, _x, _y, _z;

    /**
     * @param charObjId
     * @param boatObjId
     * @param x
     * @param y
     * @param z
     */
    public GetOffVehicle(int charObjId, int boatObjId, int x, int y, int z)
    {
        _charObjId = charObjId;
        _boatObjId = boatObjId;
        _x = x;
        _y = y;
        _z = z;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    final void writeImpl()
    {
        writeC(0x5d);
        writeD(_charObjId);
        writeD(_boatObjId);
        writeD(_x);
        writeD(_y);
        writeD(_z);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        // TODO Auto-generated method stub
        return "[S] 5d GetOffVehicle";
    }
}