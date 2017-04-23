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

import net.sf.l2j.loginserver.GameServerTable;
import net.sf.l2j.loginserver.L2LoginClient;
import net.sf.l2j.loginserver.serverpackets.LoginFail;

/**
 * Format: ddc d: fist part of session id d: second part of session id c: ? (session ID is sent in LoginOk packet and fixed to 0x55555555 0x44444444)
 */
public class RequestServerList extends ClientBasePacket
{
	private final int _key1;
	private final int _key2;
	private final int _data3;
	
	/**
	 * @return
	 */
	public int getKey1()
	{
		return _key1;
	}
	
	/**
	 * @return
	 */
	public int getKey2()
	{
		return _key2;
	}
	
	/**
	 * @return
	 */
	public int getData3()
	{
		return _data3;
	}
	
	public RequestServerList(byte[] rawPacket, L2LoginClient client)
	{
		super(rawPacket, client);
		_key1 = readD(); // loginOk 1
		_key2 = readD(); // loginOk 2
		_data3 = readC(); // ?
	}
	
	@Override
	public void run()
	{
		if ((getClient().getSessionKey() != null) && getClient().getSessionKey().checkLoginPair(_key1, _key2))
		{
			getClient().setHasAgreed(true);
			GameServerTable.getInstance().createServerList(getClient());
			
		}
		else
		{
			getClient().sendPacket(new LoginFail(LoginFail.REASON_ACCESS_FAILED));
		}
	}
}