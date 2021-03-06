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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.knownlist.MonsterKnownList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.MinionList;
import net.sf.l2j.util.Rnd;

/**
 * This class manages all Monsters.
 * 
 * L2MonsterInstance :<BR><BR>
 * <li>L2MinionInstance</li>
 * <li>L2RaidBossInstance </li>
 * <li>L2GrandBossInstance </li>
 * 
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public class L2MonsterInstance extends L2Attackable
{
	//private static Logger _log = Logger.getLogger(L2MonsterInstance.class.getName());

    protected MinionList minionList = null;

    @SuppressWarnings("unused")
    protected ScheduledFuture maintainTask = null;

    private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;

	/**
	 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 *  
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 * 
	 * @param objectId Identifier of the object to initialized
	 * @param L2NpcTemplate Template to apply to the NPC
	 */
	public L2MonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
                getKnownList();
                if (getTemplate().getMinionData() != null)
                        minionList = new MinionList(this);
	}	

        public final MonsterKnownList getKnownList()
        {
                if (super.getKnownList() == null || !(super.getKnownList() instanceof MonsterKnownList))
                        setKnownList(new MonsterKnownList(this));
                return (MonsterKnownList)super.getKnownList();
        }

	/**
	 * Return True if the attacker is not another L2MonsterInstance.<BR><BR>
	 */
	public boolean isAutoAttackable(L2Character attacker) 
	{
		if (attacker instanceof L2MonsterInstance)
			return false;
		
		return !isEventMob;
	}

	/**
	 * Return True if the L2MonsterInstance is Agressive (aggroRange > 0).<BR><BR>
	 */
	public boolean isAggressive()
	{
		return (getTemplate().aggroRange > 0) && !isEventMob;
	}

	public void onSpawn()
	{
            super.onSpawn();
        
            if (minionList != null)
            {
    		try
    	        {
                    for (L2MinionInstance minion : getSpawnedMinions())
                    {
                        if (minion == null) continue;
                        getSpawnedMinions().remove(minion);
                        minion.deleteMe();
                    }
                    minionList.clearRespawnList();
    		}
    		catch (NullPointerException e) {}
            }
            startMaintenanceTask();
	}
    
    protected int getMaintenanceInterval()
    {
        return MONSTER_MAINTENANCE_INTERVAL;
    }

    /**
     * Spawn all minions at a regular interval
     *
     */
    protected void startMaintenanceTask()
    {
        if (minionList == null)
            return;

        maintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
        {
            public void run()
            {
                minionList.spawnMinions();
            }
        }, getMaintenanceInterval());
    }

    public void callMinions()
    {
        if (minionList != null && minionList.hasMinions())
        {
            for (L2MinionInstance minion : minionList.getSpawnedMinions())
            {
                // Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance
                if (!isInsideRadius(minion, 200, false, false))
                {
                    // Get the coords of the master to use as a base to move the minion to
                    int masterX = getX();
                    int masterY = getY();
                    int masterZ = getZ();
                    
                    // Calculate a new random coord for the minion based on the master's coord
                    int minionX = masterX + (Rnd.nextInt(401) - 200);
                    int minionY = masterY + (Rnd.nextInt(401) - 200);
                    int minionZ = masterZ;
                    while (((minionX != (masterX + 30)) && (minionX != (masterX - 30))) || ((minionY != (masterY + 30)) && (minionY != (masterY - 30))))
                    {
                        minionX = masterX + (Rnd.nextInt(401) - 200);
                        minionY = masterY + (Rnd.nextInt(401) - 200);
                    }

                    // Move the minion to the new coords
                    if (!minion.isInCombat() && !minion.isDead() && !minion.isMovementDisabled())
                        minion.moveToLocation(minionX, minionY, minionZ, 0);
                }
            }
        }
    }

    public void callMinionsToAssist(L2Character attacker)
    {
        if (minionList != null && minionList.hasMinions())
        {
            List<L2MinionInstance> spawnedMinions = minionList.getSpawnedMinions();
            if (spawnedMinions != null && spawnedMinions.size() > 0)
            {
                Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
                L2MinionInstance minion;
                while (itr.hasNext())
                {
                    minion = itr.next();
                    // Trigger the aggro condition of the minion
                    if (minion != null && attacker != null && !minion.isDead() && !minion.isInCombat())
                    {
                        if (isRaid() && !(this instanceof L2MinionInstance))
                            minion.addDamageHate(attacker, 0, 100);
                        else
                            minion.addDamageHate(attacker, 0, 1);
                    }
                }
            }
        }
    }

    public boolean doDie(L2Character killer) 
    {
        if (!super.doDie(killer))
            return false;

        if (maintainTask != null)
            maintainTask.cancel(true); // doesn't do it?

        if (hasMinions() && isRaid())
            DeleteSpawnedMinions();
        return true;
    }

    public List<L2MinionInstance> getSpawnedMinions()
    {
        if (minionList == null)
            return null;

        return minionList.getSpawnedMinions();
    }
    
    public int getTotalSpawnedMinionsInstances()
    {
        if (minionList == null)
            return 0;

        return minionList.countSpawnedMinions();
    }
    
    public int getTotalSpawnedMinionsGroups()
    {
        if (minionList == null)
            return 0;

        return minionList.lazyCountSpawnedMinionsGroups();
    }
    
    public void notifyMinionDied(L2MinionInstance minion)
    {
        if (minionList == null)
            return;

        minionList.moveMinionToRespawnList(minion);
    }
    
    public void notifyMinionSpawned(L2MinionInstance minion)
    {
        if (minionList == null)
            return;

        minionList.addSpawnedMinion(minion);
    }
    
    public boolean hasMinions()
    {
        if (minionList == null)
            return false;

        return minionList.hasMinions();
    }

    public void deleteMe()
    {
        if (hasMinions())
        {
            if (maintainTask != null)
                maintainTask.cancel(true);

            DeleteSpawnedMinions();
        }
        super.deleteMe();
    }

    public void DeleteSpawnedMinions()
    {
        if (getSpawnedMinions() != null)
        {
            for (L2MinionInstance minion : getSpawnedMinions())
            {
                if (minion == null)
                    continue;

                minion.abortAttack();
                minion.abortCast();
                minion.deleteMe();
                getSpawnedMinions().remove(minion);
            }

            minionList.clearRespawnList();
        }
    }
}