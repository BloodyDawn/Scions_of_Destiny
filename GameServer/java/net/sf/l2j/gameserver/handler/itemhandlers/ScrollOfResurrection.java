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
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/** 
 * This class ... 
 * 
 * @version $Revision: 1.1.2.2.2.7 $ $Date: 2005/04/05 19:41:13 $ 
 */
public class ScrollOfResurrection implements IItemHandler 
{ 
    // all the items ids that this handler knows 
    private final static int[] _itemIds =
    {
        737,
        3936,
        3959,
        6387
    }; 

    /* (non-Javadoc) 
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance) 
     */ 
    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
        if (!(playable instanceof L2PcInstance))
            return;

        L2PcInstance activeChar = (L2PcInstance)playable;

        if (activeChar.isAllSkillsDisabled())
            return;

        L2Character target = (L2Character) activeChar.getTarget();
        boolean res = true;

        if (target == null)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_CANT_FOUND));
            res = false;
        }

        if (target != null)
        {
            if (!target.isDead())
            {
                activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                res = false;
            }
            else if (!(target instanceof L2PcInstance) && !(target instanceof L2PetInstance))
            {
                activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                res = false;
            }
        }

        if (activeChar.isSitting())
            res = false;

        if (activeChar.isFestivalParticipant())
        {
            res = false;
            activeChar.sendMessage("Resurrection inside festival is prohibited.");
        }

        if (activeChar.isInOlympiadMode())
        {
            res = false;
            activeChar.sendMessage("You cannot use this item during an Olympiad match.");
        }

        int skillId = 0;
        switch (item.getItemId())
        {
            case 737:
                skillId = 2014; // Scroll of Resurrection
                break;
            case 3936:
                skillId = 2049; // Blessed Scroll of Resurrection
                break;
            case 3959:
                skillId = 2062; // L2Day - Blessed Scroll of Resurrection
                break;
            case 6387:
                if (!(target instanceof L2PetInstance))
                    res = false;
                skillId = 2179; // Blessed Scroll of Resurrection: For Pets
                break;
        }

        if (res)
        {
            if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, true))
                return;

            L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
            if (skill != null)
                activeChar.useMagic(skill, true, true);
        }
    }

    public int[] getItemIds()
    {
        return _itemIds;
    }
}