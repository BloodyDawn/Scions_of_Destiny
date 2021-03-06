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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.skills.Stats;

public class NpcStat extends CharStat
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public NpcStat(L2NpcInstance activeChar)
    {
        super(activeChar);

        setLevel(getActiveChar().getTemplate().level);
    }

    // =========================================================
    // Method - Public

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2NpcInstance getActiveChar()
    {
        return (L2NpcInstance)super.getActiveChar();
    }

    @Override
    public final int getMaxHp()
    {
        if (getActiveChar().getMaxSiegeHp() > 0)
            return getActiveChar().getMaxSiegeHp();

        return (int)calcStat(Stats.MAX_HP, getActiveChar().getTemplate().baseHpMax, null, null);
    }

    @Override
    public int getWalkSpeed()
    {
        return (int) calcStat(Stats.WALK_SPEED, getActiveChar().getTemplate().baseWalkSpd, null, null);
    }

    @Override
    public float getMovementSpeedMultiplier()
    {
        if (getActiveChar() == null)
            return 1;

        if (getActiveChar().isRunning())
            return getRunSpeed() * 1f / getActiveChar().getTemplate().baseRunSpd;
        else
            return getWalkSpeed() * 1f / getActiveChar().getTemplate().baseWalkSpd;
    }
}