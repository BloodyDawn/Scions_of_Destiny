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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.serverpackets.BeginRotation;
import net.sf.l2j.gameserver.serverpackets.StopRotation;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author decad
 * 
 * Implementation of the Bluff Effect
 */
final class EffectBluff extends L2Effect
{
    public EffectBluff(Env env, EffectTemplate template)
    {
        super(env, template);
    }

    public EffectType getEffectType()
    {
        return EffectType.BLUFF;
    }

    public void onStart()
    {
        if (getEffected() instanceof L2FolkInstance)
            return;

        if (getEffected() instanceof L2NpcInstance && ((L2NpcInstance)getEffected()).getNpcId() == 12024)
            return;

        if (getEffected() instanceof L2SiegeSummonInstance)
            return;

        getEffected().broadcastPacket(new BeginRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
        getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
        getEffected().setHeading(getEffector().getHeading());
        getEffected().setTarget(null);
        getEffected().abortAttack();
        getEffected().abortCast();
        getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, getEffector());
    }

    /**
     * 
     * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
     */
    public boolean onActionTime()
    {
        return false;
    }
}