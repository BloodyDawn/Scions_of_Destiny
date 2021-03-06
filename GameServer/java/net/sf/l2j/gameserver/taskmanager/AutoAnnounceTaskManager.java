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
package net.sf.l2j.gameserver.taskmanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * 
 * @author nBd
 */
public class AutoAnnounceTaskManager
{
    protected static final Logger _log = Logger.getLogger(AutoAnnounceTaskManager.class.getName());

    private static AutoAnnounceTaskManager _instance;
    protected List<AutoAnnouncement> _announces = new FastList<>();

    public static AutoAnnounceTaskManager getInstance()
    {
        if (_instance == null)
            _instance = new AutoAnnounceTaskManager();

        return _instance;
    }

    public AutoAnnounceTaskManager()
    {
        restore();
    }

    public void restore()
    {
        if (!_announces.isEmpty())
        {
            for (AutoAnnouncement a : _announces)
                a.stopAnnounce();

            _announces.clear();
        }

        java.sql.Connection con = null;
        int count = 0;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT id, initial, delay, cycle, memo FROM auto_announcements");
            ResultSet data = statement.executeQuery();

            while(data.next())
            {
                int id = data.getInt("id");
                long initial = data.getLong("initial");
                long delay = data.getLong("delay");
                int repeat = data.getInt("cycle");
                String memo = data.getString("memo");
                String[] text = memo.split("/n");
                ThreadPoolManager.getInstance().scheduleGeneral(new AutoAnnouncement(id, delay, repeat, text), initial);
            }
            data.close();
            statement.close();
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "AutoAnnouncements: Failed to load announcements data.", e);
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e) {}
        }
        _log.log(Level.INFO, "AutoAnnouncements: Loaded "+_announces.size()+" Auto Announcement Data.");
    }

    private class AutoAnnouncement implements Runnable
    {
        private int _id;
        private long _delay;
        private int _repeat = -1;
        private String[] _memo;
        private boolean _stopped = false;

        public AutoAnnouncement(int id, long delay, int repeat, String[] memo)
        {
            _id = id;
            _delay = delay;
            _repeat = repeat;
            _memo = memo;
            if (!_announces.contains(this))
                _announces.add(this);
        }

        public void stopAnnounce()
        {
            _stopped = true;
        }

        public void run()
        {
            for (String text : _memo)
                announce(text);

            if (!_stopped && _repeat > 0)
                ThreadPoolManager.getInstance().scheduleGeneral(new AutoAnnouncement(_id, _delay, _repeat--, _memo), _delay);
        }
    }

    public void announce(String text)
    {
        Broadcast.announceToOnlinePlayers(text);
        _log.warning("AutoAnnounce: " + text);
    }
}