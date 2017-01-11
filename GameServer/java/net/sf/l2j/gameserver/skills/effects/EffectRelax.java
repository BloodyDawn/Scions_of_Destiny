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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;

class EffectRelax extends L2Effect
{		
    public EffectRelax(Env env, EffectTemplate template)
    {
        super(env, template);
    }

    public EffectType getEffectType()
    {
        return EffectType.RELAXING;
    }

    /** Notify started */
    public void onStart()
    {
        if (getEffected().getCurrentHp() == getEffected().getMaxHp())
        {
            if (getSkill().isToggle())
            {
                getEffected().sendPacket(new SystemMessage(175));
                return;
            }
        }

        if (getEffected() instanceof L2PcInstance)
        {
            ((L2PcInstance)getEffected()).setRelax(true);
            ((L2PcInstance)getEffected()).sitDown();
        }

        super.onStart();
    }

    public void onExit()
    {
        if (getEffected() instanceof L2PcInstance)
            ((L2PcInstance)getEffected()).setRelax(false);

        super.onExit();
    }

    public boolean onActionTime()
    {
        if (getEffected().isDead())
            return false;

        if (!((L2PcInstance)getEffected()).isSitting())
            return false;

        if (getEffected().getCurrentHp() == getEffected().getMaxHp())
        {
            if (getSkill().isToggle())
            {
                getEffected().sendPacket(new SystemMessage(175));
                return false;
            }
        }

        double manaDam = calc();
        if (manaDam > getEffected().getCurrentMp())
        {
            if (getSkill().isToggle())
            {
                getEffected().sendPacket(new SystemMessage(140));
                return false;
            }
        }

        getEffected().reduceCurrentMp(manaDam);
        return true;
    }
}