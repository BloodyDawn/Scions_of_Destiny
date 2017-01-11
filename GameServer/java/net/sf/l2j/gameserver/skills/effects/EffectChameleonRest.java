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
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;

class EffectChameleonRest extends L2Effect
{
    public EffectChameleonRest(Env env, EffectTemplate template)
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
        if (getEffected() instanceof L2PcInstance)
        {
            ((L2PcInstance)getEffected()).setRelax(true);
            ((L2PcInstance)getEffected()).setSilentMoving(true);
            ((L2PcInstance)getEffected()).sitDown();
        }
        else
            getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_REST);

        super.onStart();
    }

    /** Notify exited */
    public void onExit()
    {
        if (getEffected() instanceof L2PcInstance)
        {
            ((L2PcInstance)getEffected()).setRelax(false);
            ((L2PcInstance)getEffected()).setSilentMoving(false);
        }

        super.onExit();
    }

    public boolean onActionTime()
    {
        boolean retval = true;
        L2PcInstance effected = (L2PcInstance)getEffected();

        if (getEffected().isDead())
            retval = false;
        
        // Only cont skills shouldn't end
        if (getSkill().getSkillType() != SkillType.CONT)
            return false;        

        if (!effected.isSitting())
            retval = false;

        double manaDam = calc();
        if (manaDam > effected.getCurrentMp())
        {
            SystemMessage sm = new SystemMessage(140);
            effected.sendPacket(sm);
            return false;
        }

        if (!retval)
            effected.setRelax(retval);
        else
            effected.reduceCurrentMp(manaDam);

        return retval;
    }
}