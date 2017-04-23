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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.Manager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.EventEngine;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.PlaySound;
import net.sf.l2j.gameserver.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.serverpackets.SignsSky;
import net.sf.l2j.gameserver.serverpackets.SunRise;
import net.sf.l2j.gameserver.serverpackets.SunSet;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - admin = shows menu
 * 
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminAdmin implements IAdminCommandHandler
{
    private static String[] _adminCommands =
    {
        "admin_admin","admin_play_sounds","admin_play_sound",
        "admin_gmliston","admin_gmlistoff","admin_silence",
        "admin_atmosphere","admin_diet","admin_tradeoff",
        "admin_reload", "admin_set", "admin_saveolymp",
        "admin_endolympiad", "admin_sethero", "admin_setnoble"
    };

    private static final int REQUIRED_LEVEL = Config.GM_MENU;

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
                return false;
        }

        if (command.equals("admin_admin"))
            showMainPage(activeChar);
        else if (command.equals("admin_play_sounds"))
            AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
        else if (command.startsWith("admin_play_sounds"))
        {
            try
            {
                AdminHelpPage.showHelpPage(activeChar, "songs/songs"+command.substring(17)+".htm");
            }
            catch (StringIndexOutOfBoundsException e) {}
        }
        else if (command.startsWith("admin_play_sound"))
        {
            try
            {
                playAdminSound(activeChar,command.substring(17));
            }
            catch (StringIndexOutOfBoundsException e) {}
        }
        else if (command.startsWith("admin_gmliston"))
        {
            GmListTable.getInstance().showGm(activeChar);
            activeChar.sendMessage("Registered into gm list.");
        }
        else if (command.startsWith("admin_gmlistoff"))
        {
            GmListTable.getInstance().hideGm(activeChar);
            activeChar.sendMessage("Removed from gm list.");
        }
        else if (command.startsWith("admin_silence"))
        {     	
            if (activeChar.getMessageRefusal()) // already in message refusal mode
            {
                activeChar.setMessageRefusal(false);
                activeChar.sendPacket(new SystemMessage(SystemMessage.MESSAGE_ACCEPTANCE_MODE));
            }
            else
            {
                activeChar.setMessageRefusal(true);
                activeChar.sendPacket(new SystemMessage(SystemMessage.MESSAGE_REFUSAL_MODE));
            }	    
        }
        else if (command.startsWith("admin_saveolymp"))
        {
            try 
            {
                Olympiad.getInstance().save();
                activeChar.sendMessage("Olympiad data saved!!");
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (command.startsWith("admin_endolympiad"))
        {
            try 
            {
                Olympiad.getInstance().manualSelectHeroes();
                activeChar.sendMessage("Heroes were formed.");
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else if (command.startsWith("admin_sethero"))
        {
            L2PcInstance target = activeChar;
            if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
                target = (L2PcInstance)activeChar.getTarget();

            target.setHero(target.isHero() ? false : true);
            target.broadcastUserInfo();
        }
        else if (command.startsWith("admin_setnoble"))
        {
            L2PcInstance target = activeChar;
            if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
                target = (L2PcInstance)activeChar.getTarget();

            target.setNoble(target.isNoble() ? false : true);

            if (target.isNoble())
                activeChar.sendMessage(target.getName() + " has gained Noblesse status.");
            else
                activeChar.sendMessage(target.getName() + " has lost Noblesse status.");
        }
        else if (command.startsWith("admin_atmosphere"))
        {
            try
            {
                StringTokenizer st = new StringTokenizer(command);
                st.nextToken();
                String type = st.nextToken();
                String state = st.nextToken();
                adminAtmosphere(type,state,activeChar);
            }
            catch(Exception ex) {}
        }
        else if (command.startsWith("admin_diet"))
        {
            try
            {
                if (!activeChar.getDietMode())
                {
                    activeChar.setDietMode(true);
                    activeChar.sendMessage("Diet mode on.");
                }
                else
                {
                    activeChar.setDietMode(false);
                    activeChar.sendMessage("Diet mode off.");
                }
                activeChar.refreshOverloaded();
            }
            catch(Exception ex) {}            
        }
        else if (command.startsWith("admin_tradeoff"))
        {
            try
            {
                String mode = command.substring(15);
                if (mode.equalsIgnoreCase("on"))
                {
                    activeChar.setTradeRefusal(true);
                    activeChar.sendMessage("Tradeoff enabled.");
                }
                else if (mode.equalsIgnoreCase("off"))
                {
                    activeChar.setTradeRefusal(false);
                    activeChar.sendMessage("Tradeoff disabled.");
                }
            }
            catch(Exception ex)
            {
                if (activeChar.getTradeRefusal())
                    activeChar.sendMessage("Tradeoff currently enabled.");
                else
                    activeChar.sendMessage("Tradeoff currently disabled.");
            }            
        }
        else if (command.startsWith("admin_reload"))
        {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();

            try
            {
                String type = st.nextToken();

                if (type.startsWith("multisell"))
                {
                    L2Multisell.getInstance().reload();
                    activeChar.sendMessage("All Multisells have been reloaded.");
                }
                else if (type.startsWith("teleport"))
                {
                    TeleportLocationTable.getInstance().reloadAll();
                    activeChar.sendMessage("Teleport location table has been reloaded.");
                }
                else if (type.startsWith("skill"))
                {
                    SkillTable.getInstance().reload();
                    activeChar.sendMessage("All Skills have been reloaded.");
                }
                else if (type.startsWith("npc"))
                {
                    NpcTable.getInstance().reloadAllNpc();
                    activeChar.sendMessage("All NPCs have been reloaded.");
                }
                else if (type.startsWith("htm"))
                {
                    HtmCache.getInstance().reload();
                    activeChar.sendMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage()  + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded.");

                }
                else if (type.startsWith("item"))
                {
                    ItemTable.getInstance().reload();
                    activeChar.sendMessage("All Item templates have been reloaded.");
                }
                else if (type.startsWith("config"))
                {
                    Config.load();
                    activeChar.sendMessage("All config settings have been reload");
                }
                else if (type.startsWith("instancemanager"))
                {
                    Manager.reloadAll();
                    activeChar.sendMessage("All instance managers have been reloaded.");
                }
                else if (type.startsWith("npcwalker"))
                {
                    NpcWalkerRoutesTable.getInstance().load();
                    activeChar.sendMessage("All NPC walker routes have been reloaded.");
                }
                else if (type.startsWith("quest"))
                {
                    QuestManager.getInstance().reloadAllQuests();
                    activeChar.sendMessage("All Quests have been reloaded.");
                }
                else if (type.startsWith("event"))
                {
                    EventEngine.load();
                    activeChar.sendMessage("All Events have been reloaded.");
                }
            }
            catch(Exception e)
            {
                activeChar.sendMessage("Usage:  //reload <multisell|skill|npc|htm|item|instancemanager>");
            }
        }
        else if (command.startsWith("admin_set"))
        {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();

            try
            {
                String[] parameter = st.nextToken().split("=");

                String pName = parameter[0].trim();
                String pValue = parameter[1].trim();

                if (Config.setParameterValue(pName, pValue))
                    activeChar.sendMessage("Parameter set succesfully.");
                else
                    activeChar.sendMessage("Invalid parameter!");
            }
            catch(Exception e)
            {
                activeChar.sendMessage("Usage:  //set parameter=value");
            }
        }
        return true;
    }

    public String[] getAdminCommandList()
    {
        return _adminCommands;
    }

    private boolean checkLevel(int level) 
    {
        return (level >= REQUIRED_LEVEL);
    }

    /**
     * 
     * @param type - atmosphere type (signssky,sky)
     * @param state - atmosphere state(night,day)
     */
    public void adminAtmosphere(String type, String state, L2PcInstance activeChar)
    {
        L2GameServerPacket packet = null;

        if (type.equals("signsky"))
        {
            if (state.equals("dawn"))
                packet = new SignsSky(2);
            else if (state.equals("dusk"))
                packet = new SignsSky(1);
        }
        else if(type.equals("sky"))
        {
            if (state.equals("night"))
                packet = new SunSet();
            else if (state.equals("day"))
                packet = new SunRise();
        }
        else
            activeChar.sendMessage("Only sky and signsky atmosphere type allowed, damn u!");

        if (packet != null)
        {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers())
                player.sendPacket(packet);
        }
    }

    public void playAdminSound(L2PcInstance activeChar, String sound)
    {
        PlaySound _snd = new PlaySound(1,sound,0,0,0,0,0);
        activeChar.sendPacket(_snd);
        activeChar.broadcastPacket(_snd);
        showMainPage(activeChar);
        activeChar.sendMessage("Playing "+sound+".");
    }

    public void showMainPage(L2PcInstance activeChar)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(5);
        html.setFile("data/html/admin/adminpanel.htm");
        activeChar.sendPacket(html);
    }
}