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
package net.sf.l2j.gameserver.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.entity.TvTEvent;

public class EventEngine
{
    public static void load()
    {
        // Load all Events and their settings
        Properties eventSettings = new Properties();
        try (InputStream is = new FileInputStream(new File(Config.EVENTS_CONFIG_FILE)))
        {
            eventSettings.load(is);
        }
        catch (Exception e)
        {
            System.err.println("Error while loading Events Settings.");
            e.printStackTrace();
        }

        // TvT Event
        TvTEvent.initialize(eventSettings);
    }
}