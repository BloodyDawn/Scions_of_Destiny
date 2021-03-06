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
package net.sf.l2j.loginserver.serverpackets;

import net.sf.l2j.loginserver.SessionKey;

/**
 * Format: dddddddd f: the session key d: ? d: ? d: ? d: ? d: ? d: ?
 */
public class LoginOk extends ServerBasePacket
{
	public LoginOk(SessionKey sessionKey)
	{
		writeC(0x03);
		writeD(sessionKey.loginOkID1);
		writeD(sessionKey.loginOkID2);
		writeD(0x00);
		writeD(0x00);
		writeD(0x000003ea);
		writeD(0x00);
		writeD(0x00);
		writeD(0x02);
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}