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
package net.sf.l2j.gameserver.model.zone;

import java.util.List;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.util.Rnd;

/**
 * Abstract zone with spawn locations
 * @author DS
 *
 */
public abstract class L2ZoneSpawn extends L2ZoneType
{
    private List<Location> _spawnLocs = null;
    private List<Location> _chaoticSpawnLocs = null;

    public L2ZoneSpawn(int id)
    {
        super(id);
    }

    public final void addSpawn(int x, int y, int z)
    {
        if (_spawnLocs == null)
            _spawnLocs = new FastList<>();

        _spawnLocs.add(new Location(x, y, z));
    }

    public final void addChaoticSpawn(int x, int y, int z)
    {
        if (_chaoticSpawnLocs == null)
            _chaoticSpawnLocs = new FastList<>();

        _chaoticSpawnLocs.add(new Location(x, y, z));
    }

    public Location getSpawnLoc()
    {
        return _spawnLocs.get(Rnd.get(_spawnLocs.size()));
    }

    public Location getChaoticSpawnLoc()
    {
        if (_chaoticSpawnLocs != null)
            return _chaoticSpawnLocs.get(Rnd.get(_chaoticSpawnLocs.size()));
        else
            return getSpawnLoc();
    }
}