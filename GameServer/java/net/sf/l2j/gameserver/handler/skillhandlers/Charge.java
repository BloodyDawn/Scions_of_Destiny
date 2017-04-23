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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.1.2.2.2.9 $ $Date: 2005/04/04 19:08:01 $
 */
public class Charge implements ISkillHandler
{
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	private static SkillType[] _skillIds =
	{
		L2Skill.SkillType.CHARGE
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets, boolean crit)
	{
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (player.getCharges() >= skill.getMaxCharges())
		{
			player.sendPacket(new SystemMessage(1196));
			return;
		}
		
		player.addCharge(1);
		
		SystemMessage sm = new SystemMessage(323);
		
		sm.addNumber(player.getCharges());
		
		activeChar.sendPacket(sm);
		
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}