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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class AllyLeave extends L2GameClientPacket
{
	private static final String _C__84_ALLYLEAVE = "[C] 84 AllyLeave";
	// private static Logger _log = Logger.getLogger(AllyLeave.class.getName());
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	public void runImpl()
	{
		
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		L2Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLAN_LEADER_WITHDRAW_ALLY));
			return;
		}
		
		if (clan.getAllyId() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.NO_CURRENT_ALLIANCES));
			return;
		}
		
		if (clan.getClanId() == clan.getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ALLIANCE_LEADER_CANT_WITHDRAW));
			return;
		}
		
		clan.setAllyId(0);
		clan.setAllyName(null);
		clan.setAllyCrestId(0);
		clan.setAllyJoinExpiryTime(System.currentTimeMillis() + (Config.ALT_ALLY_JOIN_DAYS_WHEN_LEAVED * 86400000)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_ALLIANCE));
		
	}
	
	@Override
	public String getType()
	{
		return _C__84_ALLYLEAVE;
	}
}