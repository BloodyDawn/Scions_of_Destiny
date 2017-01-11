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

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * This class handles following admin commands:
 * - itemcreate = show menu
 * - create_item id [num] = creates num items with respective id, if num is not specified num = 1
 * - giveitem name id [num] = gives num items to specified player name with respective id, if num is not specified num = 1
 * 
 * @version $Revision: 1.2.2.2.2.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminCreateItem implements IAdminCommandHandler
{
    private static String[] _adminCommands =
    {
        "admin_itemcreate",
        "admin_create_item",
        "admin_giveitem"
    };

    private static final int REQUIRED_LEVEL = Config.GM_CREATE_ITEM;

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
                return false;
        }

        if (command.equals("admin_itemcreate"))
            AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");	
        else if (command.startsWith("admin_create_item"))
        {
            try
            {
                String val = command.substring(17);
                StringTokenizer st = new StringTokenizer(val);
                if (st.countTokens() == 2)
                {
                    String id = st.nextToken();
                    int idval = Integer.parseInt(id);
                    String num = st.nextToken();
                    int numval = Integer.parseInt(num);
                    createItem(activeChar,activeChar,idval,numval);
                }
                else if (st.countTokens() == 1)
                {
                    String id = st.nextToken();
                    int idval = Integer.parseInt(id);
                    createItem(activeChar,activeChar,idval,1);
                }
                else
                    AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
            }
            catch (StringIndexOutOfBoundsException e)
            {
                activeChar.sendMessage("Error while creating item.");
            }
            catch (NumberFormatException nfe)
            {
                activeChar.sendMessage("Wrong number entered.");
            }

            GMAudit.auditGMAction(activeChar.getName(), command, "no-target", "");
        }
        else if (command.startsWith("admin_giveitem"))
        {
            try
            {
                String val = command.substring(14);
                StringTokenizer st = new StringTokenizer(val);
                if (st.countTokens() == 3)
                {
                    L2PcInstance target = L2World.getInstance().getPlayer(st.nextToken());
                    if (target == null)
                    {
                        activeChar.sendMessage("Target is not online.");
                        return false;
                    }

                    String id = st.nextToken();
                    int idval = Integer.parseInt(id);
                    String num = st.nextToken();
                    int numval = Integer.parseInt(num);
                    createItem(activeChar,target,idval,numval);

                    GMAudit.auditGMAction(activeChar.getName(), command, target.getName(), "");
                }
                else if (st.countTokens() == 2)
                {
                    L2PcInstance target = L2World.getInstance().getPlayer(st.nextToken());
                    if (target == null)
                    {
                        activeChar.sendMessage("Target is not online.");
                        return false;
                    }

                    String id = st.nextToken();
                    int idval = Integer.parseInt(id);
                    createItem(activeChar,target,idval,1);

                    GMAudit.auditGMAction(activeChar.getName(), command, target.getName(), "");
                }
            }
            catch (StringIndexOutOfBoundsException e)
            {
                activeChar.sendMessage("Error while creating item.");
            }
            catch (NumberFormatException nfe)
            {
                activeChar.sendMessage("Wrong number entered.");
            }
        }

        return true;
    }

    private void createItem(L2PcInstance activeChar, L2PcInstance target, int id, int num)
    {
        if (num > 20)
        {
            L2Item template = ItemTable.getInstance().getTemplate(id);
            if (!template.isStackable())
            {
                activeChar.sendMessage("This item does not stack - Creation aborted.");
                return;
            }
        }

        target.getInventory().addItem("Admin", id, num, target, null);

        ItemList il = new ItemList(target, true);
        target.sendPacket(il);

        activeChar.sendMessage("You have spawned " + num + " item(s) number " + id + " in " + target.getName() + "'s inventory.");

        if (activeChar != target)
            target.sendMessage("An Admin has spawned " + num + " item(s) number " + id + " in your inventory.");

        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);		
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("<table width=260><tr>");
        replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("<td width=180><center>Item Creation Menu</center></td>");
        replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_help itemcreation.htm\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        replyMSG.append("</tr></table>");
        replyMSG.append("<br><br>");
        replyMSG.append("<table width=270><tr><td>Item Creation Complete.<br></td></tr></table>");
        replyMSG.append("<table width=270><tr><td>You have spawned " + num + " item(s) number in " + target.getName() + "'s inventory.</td></tr></table>");
        replyMSG.append("</body></html>");

        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    private boolean checkLevel(int level)
    {
        return (level >= REQUIRED_LEVEL);
    }

    public String[] getAdminCommandList()
    {
        return _adminCommands;
    }
}