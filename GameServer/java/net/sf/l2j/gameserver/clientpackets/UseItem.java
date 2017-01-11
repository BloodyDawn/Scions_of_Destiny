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
package net.sf.l2j.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.ShowCalculator;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2ArmorType;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;

/**
 * This class ...
 * 
 * @version $Revision: 1.18.2.7.2.9 $ $Date: 2005/03/27 15:29:30 $
 */
public class UseItem extends ClientBasePacket
{
    private static Logger _log = Logger.getLogger(UseItem.class.getName());
    private static final String _C__14_USEITEM = "[C] 14 UseItem";

    private final int _objectId;

    /**
     * packet type id 0x14
     * format:		cd
     * @param decrypt
     */
    public UseItem(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _objectId = readD();
    }

    @Override
    public void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        // Flood protect UseItem
        if (!getClient().getFloodProtectors().getUseItem().tryPerformAction("use item"))
            return;

        if (activeChar.getPrivateStoreType() != 0)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
            activeChar.sendPacket(new ActionFailed());
            return;
        }

        if (activeChar.getActiveTradeList() != null)
            activeChar.cancelActiveTrade();

        L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

        if (item == null)
            return;

        if (item.isWear())
            return;

        if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
        {
            activeChar.sendPacket(new SystemMessage(148));
            return;
        }

        int itemId = item.getItemId();

        // Items that cannot be used
        if (itemId == 57) 
            return;

        // Alt game - Karma punishment // SOE
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
        {
            switch(itemId)
            {
                case 736:
                case 1538:
                case 1829:
                case 1830:
                case 3958:
                case 5858:
                case 5859:
                    return;
            }
        }

        if (activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
        {
            // You cannot do anything else while fishing
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_DO_WHILE_FISHING_3));
            return;
        }

        // Char cannot use item when dead
        if (activeChar.isDead())
        {
            SystemMessage sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
            sm.addItemName(itemId);
            activeChar.sendPacket(sm);
            sm = null;
            return;
        }

        if (activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
            return;

        if (Config.DEBUG)
            _log.finest(activeChar.getObjectId() + ": use item " + _objectId);

        if (item.isEquipable())
        {
            // No unequipping/equipping while the player is attacking or casting
            if (activeChar.isAttackingNow() || activeChar.isCastingNow())
                return;

            int bodyPart = item.getItem().getBodyPart();
            // Prevent player to remove the weapon while mounted
            if (activeChar.isMounted()
                  && (bodyPart == L2Item.SLOT_LR_HAND
                  || bodyPart == L2Item.SLOT_L_HAND
                  || bodyPart == L2Item.SLOT_R_HAND))
                return;

            // Don't allow weapon/shield equipment if wearing formal wear
            if (activeChar.isWearingFormalWear() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
            {
                activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR));
                return;
            }

            SystemMessage sm = null;

            L2Clan cl = activeChar.getClan();
            // A shield that can only be used by the members of a clan that owns a castle.
            if ((cl == null || cl.getHasCastle() == 0) && itemId == 7015)
            {
                sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                sm = null;
                return;
            }

            // A shield that can only be used by the members of a clan that owns a clan hall.
            if ((cl == null || cl.getHasHideout() == 0) && itemId == 6902)
            {
                sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                sm = null;
                return;
            }

            // The Lord's Crown used by castle lords only
            if (itemId == 6841 && (cl == null || (cl.getHasCastle() == 0 || !activeChar.isClanLeader())))
            {
                sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                sm = null;
                return;
            }

            // Castle circlets used by the members of a clan that owns a castle.
            if (itemId >= 6834 && itemId <= 6840)
            {
                if (cl == null)
                {
                    sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                    sm.addItemName(itemId);
                    activeChar.sendPacket(sm);
                    sm = null;
                    return;
                }
                else
                {
                    int circletId = CastleManager.getInstance().getCircletByCastleId(cl.getHasCastle());
                    if (circletId != itemId)
                    {
                        sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                        sm.addItemName(itemId);
                        activeChar.sendPacket(sm);
                        sm = null;
                        return;
                    }
                }
            }

            // Char cannot use pet items
            if ((item.getItem() instanceof L2Armor && item.getItem().getItemType() == L2ArmorType.PET)
                    || (item.getItem() instanceof L2Weapon && item.getItem().getItemType() == L2WeaponType.PET))
            {
                sm = new SystemMessage(600); // You cannot equip a pet item.
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                sm = null;
                return;
            }

            // Don't allow hero equipment during Olympiad
            if (activeChar.isInOlympiadMode() && (item.getItemId() >= 6611
                    && item.getItemId() <= 6621))
                return;

            // Cannot use Traveler's/Adventurer's weapons when PK Count > 0
            if (activeChar.getPkKills() > 0 && (itemId >= 7816 && itemId <= 7831))
            {
                sm = new SystemMessage(SystemMessage.S1_CANNOT_BE_USED);
                sm.addItemName(itemId);
                activeChar.sendPacket(sm);
                sm = null;
                return;
            }

            // Equip or unEquip
            boolean isEquipped = item.isEquipped();
            L2ItemInstance[] items = null;
            L2ItemInstance old = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
            if (old == null)
                old = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

            activeChar.checkSShotsMatch(item, old);

            if (isEquipped)
            {
                if (item.getEnchantLevel() > 0)
                {
                    sm = new SystemMessage(SystemMessage.EQUIPMENT_S1_S2_REMOVED);
                    sm.addNumber(item.getEnchantLevel());
                    sm.addItemName(itemId);
                }
                else
                {
                    sm = new SystemMessage(SystemMessage.S1_DISARMED);
                    sm.addItemName(itemId);
                }
                activeChar.sendPacket(sm);

                items = activeChar.getInventory().unEquipItemInBodySlotAndRecord(bodyPart);
            }
            else
            {
                int tempBodyPart = item.getItem().getBodyPart();
                L2ItemInstance tempItem = activeChar.getInventory().getPaperdollItemByL2ItemId(tempBodyPart);
                if (tempItem != null && tempItem.isWear())
                    return;
                else if (tempBodyPart == L2Item.SLOT_LR_HAND || tempBodyPart == L2Item.SLOT_L_HAND || itemId == 6408)
                {
                    // this may not remove left OR right hand equipment
                    tempItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
                    if (tempItem != null && tempItem.isWear())
                        return;

                    tempItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
                    if (tempItem != null && tempItem.isWear())
                        return;
                }
                else if (tempBodyPart == L2Item.SLOT_FULL_ARMOR)
                {
                    // this may not remove chest or leggins
                    tempItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
                    if (tempItem != null && tempItem.isWear())
                        return;

                    tempItem = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
                    if (tempItem != null && tempItem.isWear())
                        return;
                }

                if (item.getEnchantLevel() > 0)
                {
                    sm = new SystemMessage(SystemMessage.S1_S2_EQUIPPED);
                    sm.addNumber(item.getEnchantLevel());
                    sm.addItemName(itemId);
                }
                else
                {
                    sm = new SystemMessage(SystemMessage.S1_EQUIPPED);
                    sm.addItemName(itemId);
                }
                activeChar.sendPacket(sm);

                items = activeChar.getInventory().equipItemAndRecord(item);
            }
            sm = null;

            activeChar.refreshExpertisePenalty();

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItems(Arrays.asList(items));
            activeChar.sendPacket(iu);
            activeChar.broadcastUserInfo();
        }
        else
        {
            L2Weapon weaponItem = activeChar.getActiveWeaponItem();

            if (itemId == 4393)
                activeChar.sendPacket(new ShowCalculator(4393));
            else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD)
                    && ((itemId >= 6519 && itemId <= 6527) || (itemId >= 7610 && itemId <= 7613) || (itemId >= 7807 && itemId <= 7809)))
            {
                activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
                activeChar.broadcastUserInfo();

                // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipment
                ItemList il = new ItemList(activeChar, false);
                sendPacket(il);
                return;
            }
            else
            {
                IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);

                if (handler == null)
                {
                    if (Config.DEBUG)
                        _log.warning("No item handler registered for item ID " + itemId + ".");
                }
                else
                    handler.useItem(activeChar, item);
            }
        }
    }

    public String getType()
    {
        return _C__14_USEITEM;
    }
}