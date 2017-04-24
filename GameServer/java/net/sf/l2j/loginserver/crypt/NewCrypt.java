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
package net.sf.l2j.loginserver.crypt;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class ...
 * @version $Revision: 1.3.4.1 $ $Date: 2005/03/27 15:30:09 $
 */
public class NewCrypt
{
	protected static Logger _log = Logger.getLogger(NewCrypt.class.getName());
	BlowfishEngine _crypt;
	BlowfishEngine _decrypt;
	
	public NewCrypt(String key)
	{
		this(key.getBytes());
	}

	public NewCrypt(byte[] key)
	{
		_crypt = new BlowfishEngine();
		_crypt.init(true, key);
		_decrypt = new BlowfishEngine();
		_decrypt.init(false, key);
	}
	
	public static boolean verifyChecksum(byte[] raw)
	{
		return NewCrypt.verifyChecksum(raw, 0, raw.length);
	}
	
	public static boolean verifyChecksum(byte[] raw, final int offset, final int size)
	{
		// check if size is multiple of 4 and if there is more then only the checksum
		if (((size & 3) != 0) || (size <= 4))
		{
			return false;
		}
		
		long chksum = 0;
		int count = size - 4;
		long check = -1;
		int i;
		
		for (i = offset; i < count; i += 4)
		{
			check = raw[i] & 0xff;
			check |= (raw[i + 1] << 8) & 0xff00;
			check |= (raw[i + 2] << 0x10) & 0xff0000;
			check |= (raw[i + 3] << 0x18) & 0xff000000;
			
			chksum ^= check;
		}
		
		check = raw[i] & 0xff;
		check |= (raw[i + 1] << 8) & 0xff00;
		check |= (raw[i + 2] << 0x10) & 0xff0000;
		check |= (raw[i + 3] << 0x18) & 0xff000000;
		
		return check == chksum;
	}
	
	public static void appendChecksum(byte[] raw)
	{
		NewCrypt.appendChecksum(raw, 0, raw.length);
	}
	
	public static void appendChecksum(byte[] raw, final int offset, final int size)
	{
		long chksum = 0;
		int count = size - 4;
		long ecx;
		int i;
		
		for (i = offset; i < count; i += 4)
		{
			ecx = raw[i] & 0xff;
			ecx |= (raw[i + 1] << 8) & 0xff00;
			ecx |= (raw[i + 2] << 0x10) & 0xff0000;
			ecx |= (raw[i + 3] << 0x18) & 0xff000000;
			
			chksum ^= ecx;
		}
		
		ecx = raw[i] & 0xff;
		ecx |= (raw[i + 1] << 8) & 0xff00;
		ecx |= (raw[i + 2] << 0x10) & 0xff0000;
		ecx |= (raw[i + 3] << 0x18) & 0xff000000;
		
		raw[i] = (byte) (chksum & 0xff);
		raw[i + 1] = (byte) ((chksum >> 0x08) & 0xff);
		raw[i + 2] = (byte) ((chksum >> 0x10) & 0xff);
		raw[i + 3] = (byte) ((chksum >> 0x18) & 0xff);
	}
	
	public byte[] decrypt(byte[] raw) throws IOException
	{
		byte[] result = new byte[raw.length];
		int count = raw.length / 8;
		
		for (int i = 0; i < count; i++)
		{
			_decrypt.processBlock(raw, i * 8, result, i * 8);
		}
		
		return result;
	}

	public byte[] crypt(byte[] raw) throws IOException
	{
		int count = raw.length / 8;
		byte[] result = new byte[raw.length];
		
		for (int i = 0; i < count; i++)
		{
			_crypt.processBlock(raw, i * 8, result, i * 8);
		}

		return result;
	}
}