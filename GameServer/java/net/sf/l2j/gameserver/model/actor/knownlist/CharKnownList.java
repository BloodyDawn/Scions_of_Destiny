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

import java.util.Collection;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Util;

public class CharKnownList extends ObjectKnownList
{
    // =========================================================
    // Data Field
    private Map<Integer, L2PcInstance> _KnownPlayers;
    private Map<Integer, L2Summon> _knownSummons;
    private Map<Integer, Integer> _knownRelations;
    
    // =========================================================
    // Constructor
    public CharKnownList(L2Character activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean addKnownObject(L2Object object) { return addKnownObject(object, null); }
    public boolean addKnownObject(L2Object object, L2Character dropper)
    {
        if (!super.addKnownObject(object, dropper))
            return false;

        if (object instanceof L2PcInstance)
        {
            getKnownPlayers().put(object.getObjectId(), (L2PcInstance)object);
            getKnownRelations().put(object.getObjectId(), -1);
        }
        else if (object instanceof L2Summon)
            getKnownSummons().put(object.getObjectId(), (L2Summon)object);

        return true;
    }

    /**
     * Return True if the L2PcInstance is in _knownPlayer of the L2Character.<BR><BR>
     * @param player The L2PcInstance to search in _knownPlayer
     */
    public final boolean knowsThePlayer(L2PcInstance player)
    {
        return getActiveChar() == player || getKnownPlayers().containsKey(player.getObjectId());
    }

    /** Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI. */
    public final void removeAllKnownObjects()
    {
        super.removeAllKnownObjects();
        getKnownPlayers().clear();
        getKnownRelations().clear();
        getKnownSummons().clear();

        // Set _target of the L2Character to null
        // Cancel Attack or Cast
        getActiveChar().setTarget(null);

        // Cancel AI Task
        if (getActiveChar().hasAI()) getActiveChar().setAI(null);
    }
    
    public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object)) return false;
        if (object instanceof L2PcInstance)
        {
            getKnownPlayers().remove(object.getObjectId());
            getKnownRelations().remove(object.getObjectId());
        }
        else if (object instanceof L2Summon)
            getKnownSummons().remove(object.getObjectId());

        // If object is targeted by the L2Character, cancel Attack or Cast
        if (object == getActiveChar().getTarget()) getActiveChar().setTarget(null);

        return true;
    }

    @Override
    public void forgetObjects(boolean fullCheck)
    {
    	if (!fullCheck)
    	{
            for (L2PcInstance player: getKnownPlayers().values())
            {
                // Remove all objects invisible or too far
                if (!player.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(player), getActiveObject(), player, true))
                    removeKnownObject(player);
            }
            for (L2Summon summon: getKnownSummons().values())
            {
                // Remove all objects invisible or too far
                if (!summon.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(summon), getActiveObject(), summon, true))
                    removeKnownObject(summon);
            }
            return;
        }

        // Go through knownObjects
        for (L2Object object: getKnownObjects().values())
        {
            // Remove all objects invisible or too far
            if (!object.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(object), getActiveObject(), object, true))
                removeKnownObject(object);
    	}
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2Character getActiveChar() { return (L2Character)super.getActiveObject(); }
    
    public int getDistanceToForgetObject(L2Object object) { return 0; }

    public int getDistanceToWatchObject(L2Object object) { return 0; }

    public Collection<L2Character> getKnownCharacters()
    {
        FastList<L2Character> result = new FastList<>();
        
        for (L2Object obj : getKnownObjects().values())  
        {  
            if (obj instanceof L2Character)
                result.add((L2Character)obj);  
        }
        
        return result;
    }
    
    public Collection<L2Character> getKnownCharactersInRadius(long radius)
    {
       FastList<L2Character> result = new FastList<>();
       
       for (L2Object obj : getKnownObjects().values())  
       {  
           if (obj instanceof L2Character)  
           {  
               if (Util.checkIfInRange((int)radius, getActiveChar(), obj, true))  
                   result.add((L2Character)obj);  
           }
       }
       
       return result;
    }

    public final Map<Integer, L2PcInstance> getKnownPlayers()
    {
        if (_KnownPlayers == null) _KnownPlayers = new FastMap<Integer, L2PcInstance>().shared();
        return _KnownPlayers;
    }

    public final Map<Integer, Integer> getKnownRelations()
    {
        if (_knownRelations == null) _knownRelations = new FastMap<Integer, Integer>().shared();
        return _knownRelations;
    }

    public final Map<Integer, L2Summon> getKnownSummons()
    {
        if (_knownSummons == null) _knownSummons = new FastMap<Integer, L2Summon>().shared();
        return _knownSummons;
    }

    public final Collection<L2PcInstance> getKnownPlayersInRadius(long radius)
    {
        FastList<L2PcInstance> result = new FastList<>();

        for (L2PcInstance player : getKnownPlayers().values())
        {
            if (Util.checkIfInRange((int)radius, getActiveChar(), player, true))
                result.add(player);
        }
        return result;
    }
}