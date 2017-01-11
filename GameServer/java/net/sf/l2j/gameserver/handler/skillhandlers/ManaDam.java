/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * Class handling the Mana damage skill
 *
 * @author slyce
 */
public class ManaDam implements ISkillHandler
{
    private static SkillType[] _skillIds =
    {
        SkillType.MANADAM
    };

    /**
     * 
     * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
     */
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if (activeChar.isAlikeDead())
            return;

        boolean ss = false;
        boolean bss = false;

        if (activeChar instanceof L2NpcInstance)
        {
            ss = ((L2NpcInstance)activeChar).isUsingShot(true);
            bss = ((L2NpcInstance)activeChar).isUsingShot(false);
        }

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character)targets[index];
            if (target.isInvul())
                return;

            double damage = Formulas.getInstance().calcManaDam(activeChar, target, skill, ss, bss);
            if (damage > 0)
            {
                double mp = (damage > target.getCurrentMp() ? target.getCurrentMp() : damage);
                target.reduceCurrentMp(mp);

                if (target instanceof L2PcInstance)
                {
                    StatusUpdate sump = new StatusUpdate(target.getObjectId());
                    sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
                    // [L2J_JP EDIT START - TSL]
                    target.sendPacket(sump);

                    SystemMessage sm = new SystemMessage(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1);
                    sm.addString(activeChar.getName());
                    sm.addNumber((int) mp);
                    target.sendPacket(sm);
                }
            }

            // [L2J_JP EDIT END - TSL]
        }
    }

    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}