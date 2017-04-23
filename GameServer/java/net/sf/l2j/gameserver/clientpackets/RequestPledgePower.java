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
package net.sf.l2j.gameserver.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ManagePledgePower;

public class RequestPledgePower extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(ManagePledgePower.class.getName());
	private static final String _C__C0_REQUESTPLEDGEPOWER = "[C] C0 RequestPledgePower";
	
	private int _clanMemberId;
	private int _action;
	private int _privs;
	
	@Override
	protected void readImpl()
	{
		_clanMemberId = readD();
		_action = readD();
		
		if (_action == 3)
		{
			_privs = readD();
		}
		else
		{
			_privs = 0;
		}
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.getClan() != null)
		{
			L2PcInstance member = null;
			if (player.getClan().getClanMember(_clanMemberId) != null)
			{
				member = player.getClan().getClanMember(_clanMemberId).getPlayerInstance();
			}
			
			switch (_action)
			{
				case 1:
				{
					player.sendPacket(new ManagePledgePower(player.getClanPrivileges()));
					break;
				}
				
				case 2:
				{
					
					if (member != null)
					{
						player.sendPacket(new ManagePledgePower(member.getClanPrivileges()));
					}
					
					break;
					
				}
				case 3:
				{
					
					if (player.isClanLeader())
					
					{
						
						if (member != null)
						{
							member.setClanPrivileges(_privs);
						}
						
					}
					
					break;
					
				}
			}
			
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__C0_REQUESTPLEDGEPOWER;
	}
}