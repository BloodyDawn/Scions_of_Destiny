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
package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * 
 * @author DaRkRaGe
 * Revised by Emperorc
 */
public class GrandBossManager
{
    /* =========================================================
     * This class handles all Grand Bosses:
     * <ul>
     * <li>12001    Queen Ant</li>
     * <li>12052    Core</li>
     * <li>12169    Orfen</li>
     * <li>12211    Antharas</li>
     * <li>12372    Baium</li>
     * <li>12374    Zaken</li>
     * <li>12899    Valakas</li>
     * </ul>
     * 
     * It handles the saving of hp, mp, location, and status 
     * of all Grand Bosses. It also manages the zones associated 
     * with the Grand Bosses. 
     * NOTE: The current version does NOT spawn the Grand Bosses,
     * it just stores and retrieves the values on reboot/startup,
     * for AI scripts to utilize as needed. 
     */

    public static Logger _log = Logger.getLogger(GrandBossManager.class.getName());

    private static GrandBossManager _instance;
    protected static Map<Integer, L2GrandBossInstance> _bosses;
    protected static Map<Integer, StatsSet> _storedInfo;
    private Map<Integer, Integer> _bossStatus;

    private FastList<L2BossZone> _zones;

    public static GrandBossManager getInstance()
    {
        if (_instance == null)
        {
            _log.info("Initializing GrandBossManager");
            _instance = new GrandBossManager();
        }
        return _instance;
    }

    public GrandBossManager()
    {
        init();
    }

    private void init()
    {
        _zones = new FastList<>();

        _bosses = new FastMap<>();
        _storedInfo = new FastMap<>();
        _bossStatus = new FastMap<>();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * from grandboss_data ORDER BY boss_id");
            ResultSet rset = statement.executeQuery())
        {
            while (rset.next())
            {
                // Read all info from DB, and store it for AI to read and decide what to do
                // faster than accessing DB in real time
                StatsSet info = new StatsSet();
                int bossId = rset.getInt("boss_id");
                info.set("loc_x", rset.getInt("loc_x"));
                info.set("loc_y", rset.getInt("loc_y"));
                info.set("loc_z", rset.getInt("loc_z"));
                info.set("heading", rset.getInt("heading"));
                info.set("respawn_time", rset.getLong("respawn_time"));
                double HP = rset.getDouble("currentHP"); //jython doesn't recognize doubles
                int true_HP = (int) HP;                  //so use java's ability to type cast
                info.set("currentHP", true_HP);          //to convert double to int
                double MP = rset.getDouble("currentMP");
                int true_MP = (int) MP;
                info.set("currentMP", true_MP);
                _bossStatus.put(bossId, rset.getInt("status"));

                _storedInfo.put(bossId, info);
                info = null;
            }

            _log.info("GrandBossManager: Loaded " + _storedInfo.size() + " Instance(s)");
        }
        catch (SQLException e)
        {
            _log.warning("GrandBossManager: Could not load grandboss_data table");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Zone Functions
     */
    
    public void initZones()
    {
        FastMap<Integer, L2BossZone> zones = new FastMap<>();

        if (_zones == null)
        {
            _log.warning("GrandBossManager: Could not read Grand Boss zone data");
            return;
        }

        for (L2BossZone zone : _zones)
        {
            if (zone == null)
                continue;

            zones.put(zone.getId(), zone);
        }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * from grandboss_list ORDER BY player_id");
            ResultSet rset = statement.executeQuery())
        {
            while (rset.next())
            {
                int id = rset.getInt("player_id");
                int zone_id = rset.getInt("zone");

                L2BossZone zone = zones.get(zone_id);
                if (zone != null)
                    zone.allowPlayerEntry(id, zone.getTimeInvade());
            }

            _log.info("GrandBossManager: Initialized " + _zones.size() + " Grand Boss Zone(s)");
        }
        catch (SQLException e)
        {
            _log.warning("GrandBossManager: Could not load grandboss_list table");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        zones.clear();
    }

    public void addZone(L2BossZone zone)
    {
        if (_zones != null)
            _zones.add(zone);
    }

    public final L2BossZone getZone(L2Character character)
    {
        if (_zones != null)
        {
            for (L2BossZone temp : _zones)
            {
                if (temp.isCharacterInZone(character))
                    return temp;
            }
        }
        return null;
    }

    public final L2BossZone getZone(int x, int y, int z)
    {
        if (_zones != null)
        {
            for (L2BossZone temp : _zones)
            {
                if (temp.isInsideZone(x, y, z))
                    return temp;
            }
        }
        return null;
    }

    public boolean checkIfInZone(String zoneType, L2Object obj)
    {
        L2BossZone temp = getZone(obj.getX(), obj.getY(), obj.getZ());
        if (temp == null)
            return false;

        return temp.getZoneName().equalsIgnoreCase(zoneType);
    }

    /*
     * The rest
     */
    
    public int getBossStatus(int bossId)
    {
        return _bossStatus.get(bossId);
    }

    public void setBossStatus(int bossId, int status)
    {
        _bossStatus.remove(bossId);
        _bossStatus.put(bossId, status);
    }

    /*
     * Adds a L2GrandBossInstance to the list of bosses.
     */
    public void addBoss(L2GrandBossInstance boss)
    {
        if (boss != null)
        {
            if (_bosses.containsKey(boss.getNpcId()))
                _bosses.remove(boss.getNpcId());
            _bosses.put(boss.getNpcId(), boss);
        }
    }

    public L2GrandBossInstance getBoss(int bossId)
    {
        return _bosses.get(bossId);
    }

    public StatsSet getStatsSet(int bossId)
    {
        return _storedInfo.get(bossId);
    }

    public void setStatsSet(int bossId, StatsSet info)
    {
        if (_storedInfo.containsKey(bossId))
            _storedInfo.remove(bossId);
        _storedInfo.put(bossId, info);
        storeToDb();
    }

    private void storeToDb()
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection())
        {
            try (PreparedStatement statement = con.prepareStatement("DELETE FROM grandboss_list"))
            {
                statement.executeUpdate();
            }

            for (L2BossZone zone : _zones)
            {
                if (zone == null)
                    continue;

                Integer id = zone.getId();
                FastList<Integer> list = zone.getAllowedPlayers();
                if (list == null)
                    continue;

                if (list.isEmpty())
                    continue;

                for (Integer player : list)
                {
                    try (PreparedStatement statement = con.prepareStatement("INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)"))
                    {
                        statement.setInt(1, player);
                        statement.setInt(2, id);
                        statement.executeUpdate();
                    }
                }
            }

            for (Integer bossId : _storedInfo.keySet())
            {
                L2GrandBossInstance boss = _bosses.get(bossId);
                StatsSet info = _storedInfo.get(bossId);

                if (boss == null || info == null)
                {
                    try (PreparedStatement statement = con.prepareStatement("UPDATE grandboss_data set status = ? where boss_id = ?"))
                    {
                        statement.setInt(1, _bossStatus.get(bossId));
                        statement.setInt(2, bossId);
                        statement.executeUpdate();
                    }
                }
                else
                {
                    try (PreparedStatement statement = con.prepareStatement("UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?"))
                    {
                        statement.setInt(1, boss.getX());
                        statement.setInt(2, boss.getY());
                        statement.setInt(3, boss.getZ());
                        statement.setInt(4, boss.getHeading());
                        statement.setLong(5, info.getLong("respawn_time"));

                        double hp = boss.getCurrentHp();
                        double mp = boss.getCurrentMp();
                        if (boss.isDead())
                        {
                            hp = boss.getMaxHp();
                            mp = boss.getMaxMp();
                        }

                        statement.setDouble(6, hp);
                        statement.setDouble(7, mp);
                        statement.setInt(8, _bossStatus.get(bossId));
                        statement.setInt(9, bossId);
                        statement.executeUpdate();
                    }
                }
            }
        }
        catch (SQLException e)
        {
            _log.warning("GrandBossManager: Could not store Grand Bosses to database:" + e);
        }
    }

    /**
     * Saves all Grand Boss info and then clears all info from memory,
     * including all schedules.
     */
    public void cleanUp()
    {
        storeToDb();

        _bosses.clear();
        _storedInfo.clear();
        _bossStatus.clear();
        _zones.clear();
    }
}