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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/27 15:30:07 $
 */
public class Potions implements IItemHandler
{
    private static int[] _itemIds =
    {
        65, 725, 726, 727, 728, 734,
        735, 1060, 1061, 1062, 1073,
        1374, 1375, 1539, 1540, 5234,
        5283, 5591, 5592, 6035, 6036
    };

    public synchronized void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
        L2PcInstance activeChar = null;
        if (playable instanceof L2PcInstance)
            activeChar = (L2PcInstance)playable;
        else if (playable instanceof L2PetInstance)
            activeChar = ((L2PetInstance)playable).getOwner();

        if (activeChar == null)
            return;

        if (activeChar.isInOlympiadMode())
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
            return;
        }

        if (activeChar.getEventTeam() > 0)
        {
            activeChar.sendMessage("You cannot use this item in the TvT Event.");
            return;
        }

        int itemId = item.getItemId();

        // Mana potions
        if (itemId == 726) // mana drug, xml: 2003
            usePotion(playable, item, 2003, 1); // configurable through xml till handler implemented
        else if (itemId == 728) // mana_potion, xml: 2005
            usePotion(playable, item, 2005, 1);
        // Healing and speed potions
        else if (itemId == 65) // red_potion, xml: 2001
            usePotion(playable, item, 2001, 1);
        else if (itemId == 725) // healing_drug, xml: 2002
        {
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
                return;
            usePotion(playable, item, 2002, 1);
        }
        else if (itemId == 727) // _healing_potion, xml: 2032
        {
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
                return;
            usePotion(playable, item, 2032, 1);
        }
        else if (itemId == 734) // quick_step_potion, xml: 2011
            usePotion(playable, item, 2011, 1);
        else if (itemId == 735) // swift_attack_potion, xml: 2012
            usePotion(playable, item, 2012, 1);
        else if (itemId == 1060 || itemId == 1073) // lesser_healing_potion, beginner's potion, xml: 2031
        {
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
                return;
            usePotion(playable, item, 2031, 1);
        }
        else if (itemId == 1061) // healing_potion, xml: 2032
        {
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
                return;
            usePotion(playable, item, 2032, 1);
        }
        else if (itemId == 1062) // haste_potion, xml: 2033
            usePotion(playable, item, 2033, 1);   
        else if (itemId == 1374) // adv_quick_step_potion, xml: 2034
            usePotion(playable, item, 2034, 1);
        else if (itemId == 1375) // adv_swift_attack_potion, xml: 2035
            usePotion(playable, item, 2035, 1);
        else if (itemId == 1539) // greater_healing_potion, xml: 2037
        {
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
                return;
            usePotion(playable, item, 2037, 1);
        }
        else if (itemId == 1540) // quick_healing_potion, xml: 2038
            usePotion(playable, item, 2038, 1);
        else if (itemId == 5234) // Mystery Potion
            usePotion(playable, item, 2103, 1);
        else if (itemId == 5283) // Rice Cake
            useCake(playable, item);
        else if (itemId == 5591 || itemId == 5592) // CP and Greater CP Potion
        {
            // Leave it here just in case of admins changing skill usage
            if (!isEffectReplaceable(activeChar, L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME, itemId))
                return;

            usePotion(playable, item, 2166, (itemId == 5591) ? 1 : 2);
        }
        else if (itemId == 6035) // Magic Haste Potion, xml: 2169
            usePotion(playable, item, 2169, 1);
        else if (itemId == 6036) // Greater Magic Haste Potion, xml: 2169
            usePotion(playable, item, 2169, 2);
    }

    private void usePotion(L2PlayableInstance activeChar, L2ItemInstance item, int magicId, int level)
    {
        L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
        if (skill != null)
        {
            if (!activeChar.isAllSkillsDisabled())
            {
                if (activeChar.isSkillDisabled(skill.getId()))
                {
                    SystemMessage sm = new SystemMessage(SystemMessage.S1_PREPARED_FOR_REUSE);
                    sm.addSkillName(skill.getId(), skill.getLevel());
                    activeChar.sendPacket(sm);
                    return;
                }
            }

            if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
                return;

            if (activeChar instanceof L2PcInstance)
            {
                SystemMessage sm = new SystemMessage(SystemMessage.USE_S1);
                sm.addItemName(item.getItemId());
                activeChar.sendPacket(sm);
            }

            if (skill.getReuseDelay() > 10)
                activeChar.disableSkill(skill.getId(), skill.getReuseDelay());

            activeChar.broadcastPacket(new MagicSkillUser(activeChar, activeChar, skill.getId(), skill.getLevel(), skill.getHitTime(), 0));

            // Apply effects
	    try
            {
	        ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
                if (handler != null)
                    handler.useSkill(activeChar, skill, new L2PlayableInstance[] {activeChar});
	    }
            catch (Exception e) {}
        }
    }

    private boolean isEffectReplaceable(L2PlayableInstance playable, Enum effectType, int itemId)
    {
        L2Effect[] effects = playable.getAllEffects();
        if (effects == null)
            return true;

        L2PcInstance activeChar = (L2PcInstance) ((playable instanceof L2PcInstance) ? playable : ((L2Summon) playable).getOwner());

        for (L2Effect e : effects)
        {
            if (e.getEffectType() == effectType)
            {
                // One can reuse pots after 2/3 of their duration is over.
                // It would be faster to check if its > 10 but that would screw custom pot durations...
                if (e.getTaskTime() > (e.getSkill().getBuffDuration()*67)/100000)
                    return true;
                SystemMessage sm = new SystemMessage(48);
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                return false;
            }
        }
        return true;
    }

    private void useCake(L2PlayableInstance playable, L2ItemInstance item)
    {
        if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
            return;

        if (playable instanceof L2PcInstance)
        {
            SystemMessage sm = new SystemMessage(SystemMessage.USE_S1);
            sm.addItemName(item.getItemId());
            playable.sendPacket(sm);
        }

        playable.broadcastPacket(new MagicSkillUser(playable, playable, 2136, 1, 0, 0));

        // Restore HP by 3%
        double hp = playable.getMaxHp() * 3 / 100.0;
        playable.setCurrentHp(hp + playable.getCurrentHp());

        // Restore MP by 5%
        double mp = playable.getMaxMp() * 5 / 100.0;
        playable.setCurrentMp(mp + playable.getCurrentMp());

        StatusUpdate su = new StatusUpdate(playable.getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, (int)playable.getCurrentHp());
        su.addAttribute(StatusUpdate.CUR_MP, (int)playable.getCurrentMp());
        playable.sendPacket(su);

        playable.sendMessage("Restored " + (int)hp + " HP and " + (int)mp + " MP.");
    }

    public int[] getItemIds()
    {
        return _itemIds;
    }
}