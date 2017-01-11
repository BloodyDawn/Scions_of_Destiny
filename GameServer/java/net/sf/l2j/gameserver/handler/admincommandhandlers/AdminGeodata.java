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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  -Nemesiss-
 */
public class AdminGeodata implements IAdminCommandHandler
{
    private static String[] _adminCommands =
    {
        "admin_geo_pos",
        "admin_geo_spawn_pos",
        "admin_geo_can_move",
        "admin_geo_can_see"
    };

    private static final int REQUIRED_LEVEL = Config.GM_MIN;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) 
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) 
            	return false;
        }

        if (command.equals("admin_geo_pos"))
        {
            int worldX = activeChar.getX();
            int worldY = activeChar.getY();
            int worldZ = activeChar.getZ();
            int geoX = GeoData.getInstance().getGeoX(worldX);
            int geoY = GeoData.getInstance().getGeoY(worldY);

            if (GeoData.getInstance().hasGeoPos(geoX, geoY))
                activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getNearestZ(geoX, geoY, worldZ));
            else
                activeChar.sendMessage("There is no geodata at this position.");
        }
        else if (command.equals("admin_geo_spawn_pos"))
        {
            int worldX = activeChar.getX();
            int worldY = activeChar.getY();
            int worldZ = activeChar.getZ();
            int geoX = GeoData.getInstance().getGeoX(worldX);
            int geoY = GeoData.getInstance().getGeoY(worldY);

            if (GeoData.getInstance().hasGeoPos(geoX, geoY))
                activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getSpawnHeight(worldX, worldY, worldZ, worldZ));
            else
                activeChar.sendMessage("There is no geodata at this position.");
        }
        else if (command.equals("admin_geo_can_move"))
        {
            L2Object target = activeChar.getTarget();
            if (target != null)
            {
                if (GeoData.getInstance().canSeeTarget(activeChar, target))
                    activeChar.sendMessage("Can move beeline.");
                else
                    activeChar.sendMessage("Can not move beeline!");
            }
            else
                activeChar.sendMessage("Incorrect Target.");
        }
        else if (command.equals("admin_geo_can_see"))
        {
            L2Object target = activeChar.getTarget();
            if (target != null)
            {
                if (GeoData.getInstance().canSeeTarget(activeChar, target))
                    activeChar.sendMessage("Can see target.");
                else
                    activeChar.sendMessage("Cannot see Target.");
            }
            else
                activeChar.sendMessage("Incorrect Target.");
        }
        else
            return false;

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
}