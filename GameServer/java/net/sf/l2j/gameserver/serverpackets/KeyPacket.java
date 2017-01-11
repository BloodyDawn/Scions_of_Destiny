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

import net.sf.l2j.gameserver.MMOConnection;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class KeyPacket extends ServerBasePacket
{
    private static final String _S__01_KEYPACKET = "[S] 01 KeyPacket";

    private byte[] _key;

    public KeyPacket(MMOConnection c)
    {
        _key = new byte[10];
        _key[0] = 0x00;

        if (c.getClient().isProtocolOk())
            _key[1] = 0x01;
        else
            _key[1] = 0x00;

        _key[2] = c.getCryptKey()[0];
        _key[3] = c.getCryptKey()[1];
        _key[4] = c.getCryptKey()[2];
        _key[5] = c.getCryptKey()[3];
        _key[6] = c.getCryptKey()[4];
        _key[7] = c.getCryptKey()[5];
        _key[8] = c.getCryptKey()[6];
        _key[9] = c.getCryptKey()[7];
    }

    final void writeImpl()
    {
        writeB(_key);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__01_KEYPACKET;
    }
}