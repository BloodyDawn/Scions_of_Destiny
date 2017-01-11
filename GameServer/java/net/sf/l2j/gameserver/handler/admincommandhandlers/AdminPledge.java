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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Pledge Manipulation
 * //pledge <create|dismiss>
 */
public class AdminPledge implements IAdminCommandHandler
{
    private static Logger _log = Logger.getLogger(AdminPledge.class.getName());

    private static String[] _adminCommands =
    {
        "admin_pledge"
    };

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (!activeChar.isGM() || activeChar.getAccessLevel() < Config.GM_ACCESSLEVEL)
                return false;
        }

        if (command.startsWith("admin_pledge"))
        {
            String action = null;
            String parameter = null;
            GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
            StringTokenizer st = new StringTokenizer(command);

            try
            {
                st.nextToken();
                action = st.nextToken(); // create|dismiss|setlevel
                parameter = st.nextToken(); // clanname|nothing|nothing|level
            }
            catch (NoSuchElementException nse)
            {
            }

            if (action != null)
            {
                if (activeChar.getTarget() == null)
                {
                    activeChar.sendMessage("Please select a target.");
                    return false;
                }

                if (!(activeChar.getTarget() instanceof L2PcInstance))
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessage.INCORRECT_TARGET));
                    return false;
                }

                L2PcInstance target = (L2PcInstance)activeChar.getTarget();

                if (parameter == null)
                    activeChar.sendMessage("Usage: //pledge <setlevel> <number>");
                else if (action.equals("create"))
                {
                    try
                    {
                        long time = target.getClanCreateExpiryTime();
                        target.setClanCreateExpiryTime(0);
                        L2Clan clan = ClanTable.getInstance().createClan(target, parameter);
                        if (clan != null)
                            activeChar.sendMessage("Clan "+parameter+" created! Leader: "+target.getName());
                        else
                        {
                            target.setClanCreateExpiryTime(time);
                            activeChar.sendMessage("There was a problem while creating the clan.");
                        }
                    }
                    catch(Exception e)
                    {
                        _log.warning("Error creating pledge by GM command: "+e);
                    }
                }
                else if (!target.isClanLeader())
                {
                    activeChar.sendMessage("Target is not a clan leader.");
                    showMainPage(activeChar);
                    return false;
                }
                else if (action.equals("dismiss"))
                {
                    ClanTable.getInstance().destroyClan(target.getClanId());
                    if (target.getClan() == null)
                        activeChar.sendMessage("Clan disbanded.");
                    else
                        activeChar.sendMessage("There was a problem while destroying the clan.");
                }
                else if (action.equals("setlevel"))
                {
                    int level = Integer.parseInt(parameter);
                    if (level >= 0 && level < 6)
                    {
                        target.getClan().changeLevel(level);
                        activeChar.sendMessage("You set level " + level + " for clan " + target.getClan().getName());
                    }
                    else
                        activeChar.sendMessage("Incorrect level.");
                }
            }
        }

        showMainPage(activeChar);
        return true;
    }

    public void showMainPage(L2PcInstance activeChar)
    {
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("<center><table width=260><tr><td width=40>");
        replyMSG.append("<button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        replyMSG.append("</td><td width=180>");
        replyMSG.append("<center>Clan Management</center>");
        replyMSG.append("</td><td width=40>");
        replyMSG.append("</td></tr></table></center><br>");
        replyMSG.append("<center>Create / Destroy / Level 0-5:</center>");
        replyMSG.append("<center><edit var=\"menu_command\" width=100 height=15></center><br>");
        replyMSG.append("<center><table><tr><td>");
        replyMSG.append("<button value=\"Create\" action=\"bypass -h admin_pledge create $menu_command\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
        replyMSG.append("<button value=\"Delete\" action=\"bypass -h admin_pledge dismiss $menu_command\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
        replyMSG.append("<button value=\"SetLevel\" action=\"bypass -h admin_pledge setlevel $menu_command\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        replyMSG.append("</table></center>");
        replyMSG.append("</body></html>");

        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    public String[] getAdminCommandList()
    {
        return _adminCommands;
    }
}