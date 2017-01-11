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

import java.util.logging.Logger;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.zone.type.L2OlympiadStadiumZone;

public class OlympiadStadiumManager
{
    protected static Logger _log = Logger.getLogger(OlympiadStadiumManager.class.getName());

    // =========================================================
    private static OlympiadStadiumManager _instance;
    public static final OlympiadStadiumManager getInstance()
    {
        if (_instance == null)
        {
            System.out.println("Initializing OlympiadStadiumManager");
            _instance = new OlympiadStadiumManager();
        }
        return _instance;
    }
    
    // =========================================================
    // Data Field
    private FastList<L2OlympiadStadiumZone> _olympiadStadiums;
    
    // =========================================================
    // Constructor
    public OlympiadStadiumManager()
    {
    }

    // Property - Public
    public void addStadium(L2OlympiadStadiumZone arena)
    {
        if (_olympiadStadiums == null)
            _olympiadStadiums = new FastList<>();

        _olympiadStadiums.add(arena);
    }

    public final L2OlympiadStadiumZone getStadium(L2Character character)
    {
        for (L2OlympiadStadiumZone temp : _olympiadStadiums)
        {
            if (temp.isCharacterInZone(character))
                return temp;
        }
        return null;
    }

    public final L2OlympiadStadiumZone getOlympiadStadiumById(int olympiadStadiumId)
    {
        for (L2OlympiadStadiumZone temp : _olympiadStadiums)
        {
            if (temp.getStadiumId() == olympiadStadiumId)
                return temp;
        }
        return null;
    }
}