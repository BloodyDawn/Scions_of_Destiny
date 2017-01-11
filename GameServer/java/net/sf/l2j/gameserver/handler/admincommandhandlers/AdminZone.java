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
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AdminZone implements IAdminCommandHandler
{
    private static final int REQUIRED_LEVEL = Config.GM_TEST;
    public static final String[] ADMIN_ZONE_COMMANDS =
    {
        "admin_zone_check"
    };

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#useAdminCommand(java.lang.String, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (activeChar == null)
            return false;

        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (activeChar.getAccessLevel() < REQUIRED_LEVEL)
                return false;
        }

        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command
        if (actualCommand.equalsIgnoreCase("admin_zone_check"))
        {
            if (activeChar.isInsideZone(L2PcInstance.ZONE_PVP))
                activeChar.sendMessage("This is a PvP zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_NOLANDING))
                activeChar.sendMessage("This is a non-landing zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_PEACE))
                activeChar.sendMessage("This is a Peace zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_SIEGE))
                activeChar.sendMessage("This is a Siege zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_MOTHERTREE))
                activeChar.sendMessage("This is a Mother Tree zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_CLANHALL))
                activeChar.sendMessage("This is a Clan Hall zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_WATER))
                activeChar.sendMessage("This is a Water zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_JAIL))
                activeChar.sendMessage("This is a Jail zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_MONSTERTRACK))
                activeChar.sendMessage("This is a Monster Track zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_NOHQ))
                activeChar.sendMessage("This is a Castle zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_UNUSED))
                activeChar.sendMessage("This zone is not used.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_BOSS))
                activeChar.sendMessage("This is a Boss zone.");
            if (activeChar.isInsideZone(L2PcInstance.ZONE_EFFECT))
                activeChar.sendMessage("This is an Effect zone.");

            activeChar.sendMessage("Closest Town: " + MapRegionTable.getInstance().getClosestTownName(activeChar));
        }
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#getAdminCommandList()
     */
    public String[] getAdminCommandList()
    {
        return ADMIN_ZONE_COMMANDS;
    }
}