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

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.zone.type.L2ArenaZone;

public class ArenaManager
{
    // =========================================================
    private static ArenaManager _Instance;
    public static final ArenaManager getInstance()
    {
        if (_Instance == null)
        {
    		System.out.println("Initializing ArenaManager");
        	_Instance = new ArenaManager();
        }
        return _Instance;
    }
    // =========================================================

    // =========================================================
    // Data Field
    private FastList<L2ArenaZone> _Arenas;
    
    // =========================================================
    // Constructor
    public ArenaManager()
    {
    }

    // Property - Public
    public void addArena(L2ArenaZone arena)
    {
    	if (_Arenas == null)
            _Arenas = new FastList<>();

        _Arenas.add(arena);
    }

    public final L2ArenaZone getArena(L2Character character)
    {
        for (L2ArenaZone temp : _Arenas)
        {
            if (temp.isCharacterInZone(character))
                return temp;
        }
        return null;
    }

    public final L2ArenaZone getArena(int arenaId)
    {
        for (L2ArenaZone temp : _Arenas)
        {
            if (temp.getId() == arenaId)
                return temp;
        }
        return null;
    }
}