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

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillSummon extends L2Skill
{
    private int npcId;
    private float expPenalty;
    private boolean isCubic;

    public L2SkillSummon(StatsSet set)
    {
        super(set);

        npcId = set.getInteger("npcId", 0); // default for undescribed skills
        expPenalty = set.getFloat ("expPenalty", 0.f);
        isCubic = set.getBool  ("isCubic", false);
    }

    public boolean checkCondition(L2Character activeChar, boolean itemOrWeapon)
    {
        if (activeChar instanceof L2PcInstance)
        {
            L2PcInstance player = (L2PcInstance)activeChar;

            if (player.inObserverMode())
                return false;

            if (!isCubic)
            {
                if (player.getPet() != null || player.isMounted())
                {
                    player.sendMessage("You already have a pet.");
                    return false;
                }

                if (player.isAttackingNow() || player.isRooted())
                {
                    player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_IN_COMBAT));
                    return false;
                }
            }

            // If summon siege golem (13), Summon Wild Hog Cannon (299), check if its ok to summon
            if ((getId() == 13 || getId() == 299) && !SiegeManager.getInstance().checkIfOkToSummon(player, false))
                return false;
        }
        return super.checkCondition(activeChar, itemOrWeapon);
    }

    public void useSkill(L2Character caster, L2Object[] targets)
    {
        if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
            return;

        L2PcInstance activeChar = (L2PcInstance) caster;

        if (npcId == 0)
            return;

        if (isCubic)
        {
            for (int index = 0;index < targets.length;index++)
            {
                if (!(targets[index] instanceof L2PcInstance))
                     continue;

                L2PcInstance target = (L2PcInstance)targets[index];
                int mastery = target.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
                if (mastery < 0)
                    mastery = 0;

                if (mastery == 0 && target.getCubics().size() > 0 && !target.getCubics().containsKey(npcId))
                {
                    //Player can have only 1 cubic - we should replace old cubic with new one
                    for (L2CubicInstance c : target.getCubics().values())
                    {
                        c.stopAction();
                        c.cancelDisappear();
                    }
                    target.getCubics().clear();
                }

                if (target.getCubics().size() > mastery || target.getCubics().containsKey(npcId))
                    continue;

                if (target == activeChar)
                    target.addCubic(npcId, getLevel(), false);
                else
                    target.addCubic(npcId, getLevel(), true);
                target.broadcastUserInfo();
            }
            return;		
        }

        L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(npcId);

        L2SummonInstance summon;
        if (summonTemplate.type.equalsIgnoreCase("L2SiegeSummon"))
            summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
        else
            summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);

        summon.setName(summonTemplate.name);
        summon.setTitle(activeChar.getName());
        summon.setExpPenalty(expPenalty);
        if (summon.getLevel() >= Experience.LEVEL.length)
        {
            summon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
            _log.warning("Summon ("+summon.getName()+") NpcID: "+summon.getNpcId()+" has a level above 78. Please rectify.");
        }
        else
            summon.getStat().setExp(Experience.LEVEL[(summon.getLevel() % Experience.LEVEL.length)]);

        summon.setCurrentHp(summon.getMaxHp());
        summon.setCurrentMp(summon.getMaxMp());
        summon.setHeading(activeChar.getHeading());
    	summon.setRunning();
        activeChar.setPet(summon);
	
    	L2World.getInstance().storeObject(summon);
        summon.spawnMe(activeChar.getX()+50, activeChar.getY()+100, activeChar.getZ());
    }
}