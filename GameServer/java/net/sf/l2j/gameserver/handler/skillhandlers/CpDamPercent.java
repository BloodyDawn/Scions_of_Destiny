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
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * Just a quick draft to support Wrath skill. Missing angle based calculation etc.
 */
public class CpDamPercent implements ISkillHandler
{
	private static final SkillType[] _skillIds =
	{
		SkillType.CPDAMPERCENT
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets, boolean crit)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		for (L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;
			if ((activeChar instanceof L2PcInstance) && (target instanceof L2PcInstance) && ((L2PcInstance) target).isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isDead())
			{
				continue;
			}
			
			int damage = (int) (target.getCurrentCp() * (skill.getPower() / 100));
			
			if (damage > 0)
			{
				activeChar.sendDamageMessage(target, damage, false, false, false);
				
				if (skill.hasEffects())
				{
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, false, false, false))
					
					{
						if (Formulas.getInstance().calculateSkillReflect(skill, activeChar, target))
						{
							target = activeChar;
						}
						
						// activate attacked effects, if any
						target.stopEffect(skill.getId());
						if (target.getFirstEffect(skill.getId()) != null)
						{
							target.removeEffect(target.getFirstEffect(skill.getId()));
						}
						
						skill.getEffects(activeChar, target);
						
					}
					
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessage.S1_WAS_UNAFFECTED_BY_S2);
						sm.addString(target.getName());
						sm.addSkillName(skill.getDisplayId());
						activeChar.sendPacket(sm);
					}
				}
				
				target.setCurrentCp(target.getCurrentCp() - damage);
				
				SystemMessage smsg = new SystemMessage(SystemMessage.S1_GAVE_YOU_S2_DMG);
				smsg.addString(activeChar.getName());
				smsg.addNumber(damage);
				target.sendPacket(smsg);
				
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.ATTACK_FAILED));
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}