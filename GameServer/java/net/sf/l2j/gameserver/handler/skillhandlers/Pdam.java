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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.BaseStats;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.L2WeaponType;

/**
 * This class ...
 * @version $Revision: 1.1.2.7.2.16 $ $Date: 2005/04/06 16:13:49 $
 */
public class Pdam implements ISkillHandler
{
	// all the items ids that this handler knowns
	private static Logger _log = Logger.getLogger(Pdam.class.getName());

	private static SkillType[] _skillIds =
	{
		SkillType.PDAM,
		SkillType.FATAL
	};

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets, boolean crit)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}

		if (Config.DEBUG)
		{
			_log.fine("Begin Skill processing in Pdam.java " + skill.getSkillType());
		}

		L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
		boolean soul = ((weapon != null) && (weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT) && (weapon.getItemType() != L2WeaponType.DAGGER));

		for (L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;

			if ((activeChar instanceof L2PcInstance) && (target instanceof L2PcInstance) && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isDead())
			{
				continue;
			}

			boolean dual = activeChar.isUsingDualWeapon();
			boolean shld = Formulas.getInstance().calcShldUse(activeChar, target);
			crit = false;
			if (skill.getBaseCritRate() > 0)
			{
				crit = Formulas.getInstance().calcCrit(skill.getBaseCritRate() * 10 * BaseStats.STR.calcBonus(activeChar));
			}

			double damage = Formulas.getInstance().calcPhysDam(activeChar, target, skill, shld, false, dual, soul);

			if (!crit && ((skill.getCondition() & L2Skill.COND_CRIT) != 0))
			{
				damage = 0;
			}

			if (crit)
			{
				damage *= 2;
			}

			if ((damage > 5000) && (activeChar instanceof L2PcInstance))
			{
				String name = "";
				if (target instanceof L2RaidBossInstance)
				{
					name = "RaidBoss ";
				}
				if (target instanceof L2NpcInstance)
				{
					name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().npcId + ")";
				}
				if (target instanceof L2PcInstance)
				{
					name = target.getName() + "(" + target.getObjectId() + ") ";
				}
				name += target.getLevel() + " lvl";
				Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") " + activeChar.getLevel() + " lvl did damage " + damage + " with skill " + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_pdam");
			}

			if (damage > 0)
			{
				activeChar.sendDamageMessage(target, (int) damage, false, crit, false);

				if (skill.hasEffects())
				{

					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, soul, false, false))

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

				target.reduceCurrentHp(damage, activeChar);
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.ATTACK_FAILED));
			}

			// Sonic Rage & Raging Force

			if (skill.getMaxCharges() > 0)
			{

				if (activeChar instanceof L2PcInstance)
				{

					if (((L2PcInstance) activeChar).getCharges() < skill.getMaxCharges())
					{
						((L2PcInstance) activeChar).addCharge(1);
					}
				}
			}

			if (skill.getId() == 343)
			{
				Formulas.getInstance().calcLethalStrike(activeChar, target, skill.getMagicLevel());
			}

			if (skill.getId() == 348)
			{
				// check for other effects
				try
				{
					ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.SPOIL);

					if (handler != null)
					{
						handler.useSkill(activeChar, skill, targets, crit);
					}
				}
				catch (Exception e)
				{
				}
			}

			L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if ((effect != null) && effect.isSelfEffect())
			{
				effect.exit();
			}

			skill.getEffectsSelf(activeChar);

		}

		if (skill.isSuicideAttack())
		{
			activeChar.doDie(null);
		}

		if (soul && (weapon != null))
		{
			weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
		}
	}

	@Override
	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}
