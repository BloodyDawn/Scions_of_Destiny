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

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 *
 */
public final class L2CastleTeleporterInstance extends L2FolkInstance
{
    private boolean _currentTask = false;

    /**
     * @param template
     */
    public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        if (actualCommand.equalsIgnoreCase("tele"))
        {
            int delay;
            if (!getTask())
            {
                if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
                    delay = 480000;
                else
                    delay = 30000;

                setTask(true);
                ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
            }

            String filename = "data/html/teleporter/MassGK-1.htm";
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile(filename);
            player.sendPacket(html);
            return;
        }
        else
            super.onBypassFeedback(player, command);
    }

    public void showChatWindow(L2PcInstance player)
    {
        String filename;
        if (!getTask())
        {
            if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
                filename = "data/html/teleporter/MassGK-2.htm";
            else
                filename = "data/html/teleporter/MassGK.htm";
        }
        else
            filename = "data/html/teleporter/MassGK-1.htm";

        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }

    public void oustAllPlayers()
    {
        getCastle().oustAllPlayers();
    }

    class oustAllPlayers implements Runnable
    {
        public void run()
        {
            try
            {
                if (getCastle().getSiege().getIsInProgress())
                {
                    CreatureSay cs = new CreatureSay(getObjectId(), 1, getName(), "The defenders of "+ getCastle().getName()+" castle will be teleported to the inner castle.");
                    int region = MapRegionTable.getInstance().getMapRegion(getX(), getY());
                    for (L2PcInstance player : L2World.getInstance().getAllPlayers())
                    {
                        if (region == MapRegionTable.getInstance().getMapRegion(player.getX(),player.getY()))
                            player.sendPacket(cs);
                    }
                }
                oustAllPlayers();
                setTask(false);
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * This is called when a player interacts with this NPC
     * @param player
     */
    public void onAction(L2PcInstance player)
    {
        if (!canTarget(player))
            return;

        player.setLastFolkNPC(this);

        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget())
        {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
            player.sendPacket(my);

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        }
        else 
        {
            // Calculate the distance between the L2PcInstance and the L2NpcInstance
            if (!canInteract(player))
            {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            }
            else
                showChatWindow(player);
        }

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendPacket(new ActionFailed());
    }

    public boolean getTask()
    {
        return _currentTask;
    }

    public void setTask(boolean state)
    {
        _currentTask = state;
    }
}