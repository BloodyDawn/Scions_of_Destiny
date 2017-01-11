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
import java.util.List;

import javolution.util.FastList;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.ClanHall;

public class ClanHallManager
{
    // =========================================================
    private static ClanHallManager _Instance;
    public static final ClanHallManager getInstance()
    {
        if (_Instance == null)
        {
    		System.out.println("Initializing ClanHallManager");
        	_Instance = new ClanHallManager();
        }
        return _Instance;
    }
    // =========================================================

    
    // =========================================================
    // Data Field
    private List<ClanHall> _ClanHalls;
    
    // =========================================================
    // Constructor
    public ClanHallManager()
    {
        load();
    }

    // =========================================================
    // Method - Private
    private final void load()
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
            ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                if (rs.getInt("ownerId") != 0)
                {
                    // just in case clan is deleted manually from db
                    if (ClanTable.getInstance().getClan(rs.getInt("ownerId")) == null)
                        AuctionManager.initNPC(rs.getInt("id"));
                }
            	getClanHalls().add(new ClanHall(rs.getInt("id"), rs.getString("name"), rs.getInt("ownerId"), rs.getInt("lease"), rs.getString("desc"), rs.getString("location"), rs.getLong("paidUntil"), rs.getInt("Grade"), rs.getBoolean("paid")));
            }

            System.out.println("Loaded: " + getClanHalls().size() + " clan halls");
        }
        catch (Exception e)
        {
            System.out.println("Exception: ClanHallManager.load(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // Property - Public
    public final ClanHall getClanHallById(int clanHallId)
    {
        for (ClanHall clanHall : getClanHalls())
        {
            if (clanHall.getId() == clanHallId)
                return clanHall;
        }
        return null;
    }

    public final ClanHall getNearbyClanHall(int x, int y, int maxDist)
    {
        for (ClanHall ch : getClanHalls())
        {
            if (ch.getZone() != null && ch.getZone().getDistanceToZone(x, y) < maxDist)
                return ch;
        }
        return null;
    }

    public final ClanHall getClanHallByOwner(L2Clan clan)
    {
        for (ClanHall clanHall : getClanHalls())
        {
            if (clan.getClanId() == clanHall.getOwnerId())

                return clanHall;
        }
        return null;
    }

    public final List<ClanHall> getClanHalls()
    {
        if (_ClanHalls == null) _ClanHalls = new FastList<>();
        return _ClanHalls;
    }
}