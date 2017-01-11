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
package net.sf.l2j.gameserver.util;

/**
 * Flood protector configuration
 * 
 * @author fordfrog
 */
public final class FloodProtectorConfig
{
    /**
     * Type used for identification of logging output.
     */
    public String FLOOD_PROTECTOR_TYPE;
    /**
     * Flood protection interval in game ticks.
     */
    public int FLOOD_PROTECTION_INTERVAL;
    /**
     * Whether flooding should be logged.
     */
    public boolean LOG_FLOODING;
    /**
     * If specified punishment limit is exceeded, punishment is applied.
     */
    public int PUNISHMENT_LIMIT;
    /**
     * Punishment type. Either 'none', 'kick', 'ban' or 'jail'.
     */
    public String PUNISHMENT_TYPE;
    /**
     * For how long should the char/account be punished.
     */
    public int PUNISHMENT_TIME;

    /**
     * Creates new instance of FloodProtectorConfig.
     * 
     * @param floodProtectorType
     *            {@link #FLOOD_PROTECTOR_TYPE}
     */
    public FloodProtectorConfig(final String floodProtectorType)
    {
        super();
        FLOOD_PROTECTOR_TYPE = floodProtectorType;
    }
}