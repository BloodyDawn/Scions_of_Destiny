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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author littlecrow
 *
 * Implementation of the Fear Effect
 */
final class EffectFear extends L2Effect
{
    public static final int FEAR_RANGE = 500;

    private int _dX = -1;
    private int _dY = -1;

    public EffectFear(Env env, EffectTemplate template)
    {
        super(env, template);
    }

    public EffectType getEffectType()
    {
        return EffectType.FEAR;
    }

    /** Notify started */
    public void onStart()
    {
        if (getEffected() instanceof L2FolkInstance)
            return;

        if (getEffected() instanceof L2NpcInstance && ((L2NpcInstance)getEffected()).getNpcId() == 12024)
            return;

        if (getEffected() instanceof L2SiegeSummonInstance)
            return;

        // players are only affected by grandboss skills
        if (getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance)
            return;

        if (!getEffected().isAfraid())
        {
            if (getEffected().getX() > getEffector().getX())
                _dX = 1;
            if (getEffected().getY() > getEffector().getY())
                _dY = 1;

            getEffected().startFear();
            onActionTime();
        }
    }

    /** Notify exited */
    public void onExit()
    {
        getEffected().stopFear(this);
    }

    public boolean onActionTime()
    {
        int posX = getEffected().getX();
        int posY = getEffected().getY();
        int posZ = getEffected().getZ();

        posX += _dX*FEAR_RANGE;
        posY += _dY*FEAR_RANGE;

        if (Config.GEODATA > 0)
        {
            Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ);
            posX = destiny.getX();
            posY = destiny.getY();
        }

        if (!(getEffected() instanceof L2PetInstance))
            getEffected().setRunning();

        getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(posX,posY,posZ,0));
        return true;
    }
}