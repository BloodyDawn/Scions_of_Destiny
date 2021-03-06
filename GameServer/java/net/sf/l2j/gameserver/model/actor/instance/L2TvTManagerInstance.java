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

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2TvTManagerInstance extends L2NpcInstance
{
    public L2TvTManagerInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
        if (command.equals("tvt_event_participation"))
            TvTEvent.registerPlayer(player);
        else if (command.equals("tvt_event_remove_participation"))
            TvTEvent.removePlayer(player);
    }

    @Override
    public void showChatWindow(L2PcInstance player, int val)
    {
        if (player == null)
            return;

        if (!TvTEvent.isEnabled)
            return;

        if (TvTEvent.getEventState() == TvTEvent.REGISTER)
        {
            String htmFile = "data/html/event/";

            if (!TvTEvent.isRegistered(player))
                htmFile += "TvTEventParticipation";
            else
                htmFile += "TvTEventRemoveParticipation";

            htmFile += ".htm";

            String htmContent = HtmCache.getInstance().getHtm(htmFile);
            if (htmContent != null)
            {
                NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

                npcHtmlMessage.setHtml(htmContent);
                npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
                npcHtmlMessage.replace("%registeredcount%", String.valueOf(TvTEvent.getRegistered().size()));
                npcHtmlMessage.replace("%minimumplayers%", String.valueOf(TvTEvent.minParticipants));
                npcHtmlMessage.replace("%maximumplayers%", String.valueOf(TvTEvent.maxParticipants));
                npcHtmlMessage.replace("%minimumlevel%", String.valueOf(TvTEvent.minLevel));
                npcHtmlMessage.replace("%maximumlevel%", String.valueOf(TvTEvent.maxLevel));
                player.sendPacket(npcHtmlMessage);
            }
        }
        else
        {
            String htmFile = "data/html/event/TvTEventStatus.htm";
            String htmContent = HtmCache.getInstance().getHtm(htmFile);

            if (htmContent != null)
            {
                
                NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

                npcHtmlMessage.setHtml(htmContent);
                npcHtmlMessage.replace("%team1playercount%", String.valueOf(TvTEvent.getBlueTeam().size()));
                npcHtmlMessage.replace("%team1points%", String.valueOf(TvTEvent.getBlueTeamKills()));
                npcHtmlMessage.replace("%team2playercount%", String.valueOf(TvTEvent.getRedTeam().size()));
                npcHtmlMessage.replace("%team2points%", String.valueOf(TvTEvent.getRedTeamKills()));
                player.sendPacket(npcHtmlMessage);
            }
        }

        player.sendPacket(new ActionFailed());
    }
}