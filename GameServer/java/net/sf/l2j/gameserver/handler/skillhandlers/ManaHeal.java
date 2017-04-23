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
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;

/**
 * This class ...
 * @version $Revision: 1.1.2.2.2.1 $ $Date: 2005/03/02 15:38:36 $
 */
public class ManaHeal implements ISkillHandler
{
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	private static SkillType[] _skillIds =
	{
		SkillType.MANAHEAL,
		SkillType.MANARECHARGE
	};
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets, boolean crit)
	{
		L2Character target = null;
		
		for (L2Object target2 : targets)
		{
			target = (L2Character) target2;
			
			double mp = (skill.getSkillType() == SkillType.MANARECHARGE) ? target.calcStat(Stats.RECHARGE_MP_RATE, skill.getPower(), null, null) : skill.getPower();
			target.setLastHealAmount((int) mp);
			target.setCurrentMp(mp + target.getCurrentMp());
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
			target.sendPacket(sump);
			
			if ((activeChar instanceof L2PcInstance) && (activeChar != target))
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S2_MP_RESTORED_BY_S1);
				sm.addString(activeChar.getName());
				sm.addNumber((int) mp);
				target.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S1_MP_RESTORED);
				sm.addNumber((int) mp);
				target.sendPacket(sm);
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}