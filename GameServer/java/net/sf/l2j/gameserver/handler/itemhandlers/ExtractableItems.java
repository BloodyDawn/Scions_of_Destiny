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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.ExtractableItemsData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ExtractableItem;
import net.sf.l2j.gameserver.model.L2ExtractableProductItem;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.util.Rnd;

/**
 * 
 * @author FBIagent 11/12/2006
 * 
 */
public class ExtractableItems implements IItemHandler
{
    private static Logger _log = Logger.getLogger(ItemTable.class.getName());
    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
        if (!(playable instanceof L2PcInstance))
        	return;

        L2PcInstance activeChar = (L2PcInstance)playable;

        int itemID = item.getItemId();
        L2ExtractableItem exitem = ExtractableItemsData.getInstance().getExtractableItem(itemID);

        if (exitem == null)
            return;
        
        int createItemID = 0;
        int createAmount = 0;
        int rndNum = Rnd.get(100);
        int chanceFrom = 0;

        // calculate extraction
        for (L2ExtractableProductItem expi : exitem.getProductItemsArray())
        {
            int chance = expi.getChance();

            if (rndNum >= chanceFrom && rndNum <= chance+chanceFrom)
            {
                createItemID = expi.getId();
                createAmount = expi.getAmmount();
                break;
            }

            chanceFrom += chance;
        }

	if (createItemID <= 0)
        {
            activeChar.sendMessage("Nothing happened.");
	    return;
        }

        if (ItemTable.getInstance().createDummyItem(createItemID) == null)
        {
            _log.warning("createItemID "+createItemID+" doesn't have template!");
            activeChar.sendMessage("Nothing happened.");
            return;
        }

        if (!activeChar.destroyItemByItemId("Extract", itemID, 1, activeChar.getTarget(), true))
            return;

        if (ItemTable.getInstance().createDummyItem(createItemID).isStackable())
            activeChar.addItem("Extract", createItemID, createAmount, item, false);
        else
        {
            for (int i = 0; i < createAmount; i++)
                activeChar.addItem("Extract", createItemID, 1, item, false);
        }

        SystemMessage sm;

        if (createAmount > 1)
        {
            sm = new SystemMessage(SystemMessage.EARNED_S2_S1_s);
            sm.addItemName(createItemID);
            sm.addNumber(createAmount);
        }
        else
        {
            sm = new SystemMessage(SystemMessage.EARNED_ITEM);
            sm.addItemName(createItemID);
        }
        activeChar.sendPacket(sm);
    }
    
    public int[] getItemIds()
    {
    	return ExtractableItemsData.getInstance().itemIDs();
    }
}