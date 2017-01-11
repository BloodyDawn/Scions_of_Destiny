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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillChargeDmg extends L2Skill 
{
	final int num_charges;

	public L2SkillChargeDmg(StatsSet set)
        {
		super(set);
		num_charges = set.getInteger("num_charges", getLevel());
	}

	public boolean checkCondition(L2Character activeChar, boolean itemOrWeapon)
	{
                L2PcInstance player = (L2PcInstance)activeChar;
                if (player.getCharges() < num_charges)
                {
                        SystemMessage sm = new SystemMessage(113);
                        sm.addSkillName(getId());
                        activeChar.sendPacket(sm);
                        return false;
		}
		return super.checkCondition(activeChar, itemOrWeapon);
	}

	public void useSkill(L2Character caster, L2Object[] targets)
        {
                L2PcInstance player = (L2PcInstance)caster;

		if (caster.isAlikeDead())
			return;

                // Formula tested by L2Guru
                double modifier = 0;
                modifier = 0.8 + 0.201 * player.getCharges();

                player.addCharge(-num_charges);

                L2ItemInstance weapon = caster.getActiveWeaponInstance();
                boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

                for (int index = 0;index < targets.length;index++)
                {
                        L2Character target = (L2Character)targets[index];
		        if (target.isAlikeDead())
		                continue;

		        boolean shld = Formulas.getInstance().calcShldUse(caster, target);

		        double damage = Formulas.getInstance().calcPhysDam(caster, target, this, shld, false, false, soul);

                        if (damage > 0)
                        {
                                damage = damage * modifier;
                                target.reduceCurrentHp(damage, caster);

                                caster.sendDamageMessage(target, (int)damage, false, false, false);
                        }
                }

                if (soul && weapon != null)
                        weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
	}
}
