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

import net.sf.l2j.loginserver.L2LoginClient;
import net.sf.l2j.loginserver.L2LoginClient.LoginClientState;
import net.sf.l2j.loginserver.serverpackets.GGAuth;
import net.sf.l2j.loginserver.serverpackets.LoginFail;

public class RequestAuthGG extends ClientBasePacket
{
	private int _sessionId = 0;
	private final int _data1;
	private final int _data2;
	private final int _data3;
	private final int _data4;

	public int getSessionId()
	{
		return _sessionId;
	}

	public int getData1()
	{
		return _data1;
	}

	public int getData2()
	{
		return _data2;
	}

	public int getData3()
	{
		return _data3;
	}

	public int getData4()
	{
		return _data4;
	}

	public RequestAuthGG(byte[] rawPacket, L2LoginClient client)
	{
		super(rawPacket, client);
		_sessionId = readD();
		_data1 = readD();
		_data2 = readD();
		_data3 = readD();
		_data4 = readD();
	}
	
	@Override
	public void run()
	{
		if (_sessionId == getClient().getSessionId())
		{
			getClient().setState(LoginClientState.AUTHED_GG);
			getClient().sendPacket(new GGAuth(_sessionId));
		}
		else
		{
			getClient().sendPacket(new LoginFail(LoginFail.REASON_ACCESS_FAILED));
		}
	}
}