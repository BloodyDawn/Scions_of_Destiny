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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcBufferTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2NpcBufferInstance extends L2FolkInstance
{
    public L2NpcBufferInstance (int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public void showChatWindow(L2PcInstance playerInstance, int val)
    {
        if (playerInstance == null)
            return;

        String htmContent = HtmCache.getInstance().getHtm("data/html/npcbuffer/NpcBuffer.htm");
        if (!Config.NPC_BUFFER_ENABLED)
            htmContent = HtmCache.getInstance().getHtm("data/html/npcdefault.htm");
        else if (val > 0)
            htmContent = HtmCache.getInstance().getHtm("data/html/npcbuffer/NpcBuffer-" + val + ".htm");

        if (htmContent != null)
        {
            NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

            npcHtmlMessage.setHtml(htmContent);
            npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
            playerInstance.sendPacket(npcHtmlMessage);
        }
        playerInstance.sendPacket(new ActionFailed());
    }

    int pageVal = 0;

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
        if (player == null || player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != getObjectId())
            return;

        if (Olympiad.getInstance().isRegisteredInComp(player))
            return;

        if (player.getEventTeam() > 0 || TvTEvent.isRegistered(player))
            return;

        if (command.startsWith("Chat"))
        {
            int val = Integer.parseInt(command.substring(5));
            pageVal = val;

            showChatWindow(player, val);
        }
        else if (command.startsWith("Buff") || command.startsWith("PetBuff"))
        {
            L2Character target = player;
            if (command.startsWith("Pet"))
            {
                if (player.getPet() == null)
                {
                    player.sendMessage("You do not have a pet.");
                    showChatWindow(player, 0); // 0 = main window
                    return;
                }
                target = player.getPet();
            }

            String[] buffGroupArray = command.substring(command.indexOf("Buff") + 5).split(" ");

            for (String buffGroupList : buffGroupArray)
            {
                if (buffGroupList == null)
                {
                    _log.warning("NPC Buffer Warning: NPC Buffer has no buffGroup set in the bypass for the buff selected.");
                    return;
                }

                int buffGroup = Integer.parseInt(buffGroupList);
                int[] buffGroupInfo = NpcBufferTable.getInstance().getSkillGroupInfo(buffGroup);

                if (buffGroupInfo == null)
                {
                    _log.warning("Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
                    return;
                }

                int skillId = buffGroupInfo[0];
                int skillLevel = buffGroupInfo[1];
                int skillFeeId = buffGroupInfo[2];
                int skillFeeAmount = buffGroupInfo[3];

                if (skillFeeId != 0)
                {
                    L2ItemInstance itemInstance = player.getInventory().getItemByItemId(skillFeeId);
                    if (itemInstance == null || (!itemInstance.isStackable() && player.getInventory().getInventoryItemCount(skillFeeId, -1) < skillFeeAmount))
                    {
                        SystemMessage sm = new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS);
                        player.sendPacket(sm);
                        continue;
                    }

                    if (itemInstance.isStackable())
                    {
                        if (!player.destroyItemByItemId("Npc Buffer", skillFeeId, skillFeeAmount, player.getTarget(), true))
                        {
                            SystemMessage sm = new SystemMessage(SystemMessage.NOT_ENOUGH_ITEMS);
                            player.sendPacket(sm);
                            continue;
                        }
                    }
                    else
                    {
                        for (int i = 0;i < skillFeeAmount;++ i)
                            player.destroyItemByItemId("Npc Buffer", skillFeeId, 1, player.getTarget(), true);
                    }
                }

                L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
                if (skill != null)
                    skill.getEffects(this, target);
            }
            showChatWindow(player, pageVal);
        }
        else if (command.startsWith("Heal"))
        {
            if (!player.isInCombat() && !AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
            {
                String[] healArray = command.substring(5).split(" ");
                for (String healType : healArray)
                {
                    if (healType.equalsIgnoreCase("HP"))
                        player.setCurrentHp(player.getMaxHp());
                    else if (healType.equalsIgnoreCase("MP"))
                        player.setCurrentMp(player.getMaxMp());
                    else if (healType.equalsIgnoreCase("CP"))
                        player.setCurrentCp(player.getMaxCp());
                }
            }
            showChatWindow(player, 0);
        }
        else if (command.startsWith("RemoveBuffs"))
        {
            player.stopAllEffects();
            showChatWindow(player, 0);
        }
        else
            super.onBypassFeedback(player, command);
    }

    @Override
    public boolean isAIOBuffer()
    {
        return true;
    }
}