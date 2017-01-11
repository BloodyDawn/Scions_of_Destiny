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
package net.sf.l2j.loginserver.clientpackets;

import net.sf.l2j.loginserver.ClientThread;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.1 $ $Date: 2005/03/27 15:30:12 $
 */
public abstract class ClientBasePacket implements Runnable
{
    private ClientThread _client;
    private byte[] _decrypt;
    private int _off;

    public ClientBasePacket(byte[] decrypt, ClientThread client)
    {
        _decrypt = decrypt;
        _off = 1;		// skip packet type id
        _client = client;
    }

    public abstract void run();

    public ClientThread getClient()
    {
        return _client;
    }

    public byte[] getByteBuffer()
    {
        return _decrypt;
    }

    public int readD()
    {
        int result = _decrypt[_off++] &0xff;
        result |= _decrypt[_off++] << 8 &0xff00;
        result |= _decrypt[_off++] << 0x10 &0xff0000;
        result |= _decrypt[_off++] << 0x18 &0xff000000;
        return result;
    }

    public int readC()
    {
        int result = _decrypt[_off++] &0xff;
        return result;
    }
}