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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.Map;

import javolution.util.FastMap;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.util.Util;

public class ObjectKnownList
{
    // =========================================================
    // Data Field
    private L2Object _ActiveObject;
    private Map<Integer, L2Object> _KnownObjects;
    
    // =========================================================
    // Constructor
    public ObjectKnownList(L2Object activeObject)
    {
        _ActiveObject = activeObject;
    }

    // =========================================================
    // Method - Public
    public boolean addKnownObject(L2Object object) { return addKnownObject(object, null); }
    public boolean addKnownObject(L2Object object, L2Character dropper)
    {
        if (object == null) return false;

        // Check if already knows object
        if (knowsObject(object))
        {
            if (getActiveObject() instanceof L2Character)
                return ((L2Character)getActiveObject()).isSummoned();

            return false;
        }

        // Check if object is not inside distance to watch object
        if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), getActiveObject(), object, true)) return false;
        
        return (getKnownObjects().put(object.getObjectId(), object) == null);
    }

    public final boolean knowsObject(L2Object object)
    {
        return getActiveObject() == object || getKnownObjects().containsKey(object.getObjectId());
    }
    
    /** Remove all L2Object from _knownObjects */
    public void removeAllKnownObjects() { getKnownObjects().clear(); }

    public boolean removeKnownObject(L2Object object)
    {
        if (object == null) return false;
        return (getKnownObjects().remove(object.getObjectId()) != null);
    }
    
    public void forgetObjects(boolean fullCheck)
    {
    	// Go through knownObjects    
    	for (L2Object object: getKnownObjects().values())
    	{
            if (!fullCheck && !(object instanceof L2PlayableInstance))
                continue;

            // Remove all objects invisible or too far
            if (!object.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(object), getActiveObject(), object, true))
                removeKnownObject(object);
    	}
    }

    // =========================================================
    // Property - Public
    public L2Object getActiveObject()
    {
        return _ActiveObject;
    }

    public int getDistanceToForgetObject(L2Object object) { return 0; }

    public int getDistanceToWatchObject(L2Object object) { return 0; }

    /** Return the _knownObjects containing all L2Object known by the L2Character. */
    public final Map<Integer, L2Object> getKnownObjects()
    {
        if (_KnownObjects == null) _KnownObjects = new FastMap<Integer, L2Object>().shared();
        return _KnownObjects;
    }
}