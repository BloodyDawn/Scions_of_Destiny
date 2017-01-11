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

import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;

public class AttackableKnownList extends NpcKnownList
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public AttackableKnownList(L2Attackable activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object))
            return false;

        // Remove the L2Object from the _aggrolist of the L2Attackable
        if (object != null && object instanceof L2Character)
            getActiveChar().getAggroList().remove(object);
        return true;
    }
    
    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2Attackable getActiveChar()
    {
        return (L2Attackable)super.getActiveChar();
    }

    public int getDistanceToForgetObject(L2Object object)
    {
        return 2 * getDistanceToWatchObject(object);
    }

    public int getDistanceToWatchObject(L2Object object)
    {
        if (object instanceof L2FolkInstance || !(object instanceof L2Character))
            return 0;

        if (object instanceof L2PlayableInstance)
            return object.getKnownList().getDistanceToWatchObject(getActiveObject());

        if ((getActiveChar().getFactionRange() + getActiveChar().getAggroRange()) > 300)
            return (getActiveChar().getFactionRange() + getActiveChar().getAggroRange());

        return 300;
    }
}