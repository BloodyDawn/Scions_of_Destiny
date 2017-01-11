/* This program is free software; you can redistribute it and/or modify
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
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;

/**
 * This class ...
 * 
 * @author earendil
 * 
 * @version $Revision: 1.1.2.2.2.4 $ $Date: 2005/04/06 16:13:48 $
 */

public class BalanceLife implements ISkillHandler
{
    private static SkillType[] _skillIds = { SkillType.BALANCE_LIFE };

    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        // check for other effects
        try
        {
            ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);

            if (handler != null)
                handler.useSkill(activeChar, skill, targets);
        }
        catch (Exception e){}

        double fullHP = 0;
        double currentHPs = 0;

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character) targets[index];

            // We should not heal if char is dead
            if (target == null || target.isDead())
                continue;

            fullHP += target.getMaxHp();
            currentHPs += target.getCurrentHp();
        }

        double percentHP = currentHPs / fullHP;

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character) targets[index];

            double newHP = target.getMaxHp() * percentHP;
            double totalHeal = newHP - target.getCurrentHp();

            target.setCurrentHp(newHP);

            if (totalHeal > 0)
                target.setLastHealAmount((int) totalHeal);

            StatusUpdate su = new StatusUpdate(target.getObjectId());
            su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
            target.sendPacket(su);

            target.sendMessage("HP of the party has been balanced.");
        }
    }

    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}