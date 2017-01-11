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
package net.sf.l2j.gameserver.ai;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.VehicleDeparture;
import net.sf.l2j.gameserver.serverpackets.VehicleInfo;
import net.sf.l2j.gameserver.serverpackets.VehicleStarted;

/**
 * @author DS
 */
public class L2BoatAI extends L2CharacterAI
{
    public L2BoatAI(L2BoatInstance.AIAccessor accessor)
    {
        super(accessor);
    }

    @Override
    protected void onIntentionAttack(L2Character target)
    {
    }

    @Override
    protected void onIntentionCast(L2Skill skill, L2Object target)
    {
    }

    @Override
    protected void onIntentionFollow(L2Character target)
    {
    }

    @Override
    protected void onIntentionPickUp(L2Object item)
    {
    }

    @Override
    protected void onIntentionInteract(L2Object object)
    {
    }

    @Override
    protected void onEvtAttacked(L2Character attacker)
    {
    }

    @Override
    protected void onEvtAggression(L2Character target, int aggro)
    {
    }

    @Override
    protected void onEvtStunned(L2Character attacker)
    {
    }

    @Override
    protected void onEvtSleeping(L2Character attacker)
    {
    }

    @Override
    protected void onEvtRooted(L2Character attacker)
    {
    }

    @Override
    protected void onEvtForgetObject(L2Object object)
    {
    }

    @Override
    protected void onEvtCancel()
    {
    }

    @Override
    protected void onEvtDead()
    {
    }

    @Override
    protected void onEvtFakeDeath()
    {
    }

    @Override
    protected void onEvtFinishCasting()
    {
    }

    @Override
    protected void clientActionFailed()
    {
    }

    @Override
    protected void moveToPawn(L2Object pawn, int offset)
    {
    }

    @Override
    protected void moveTo(int x, int y, int z)
    {
        if (!_actor.isMovementDisabled())
        {
            if (!_client_moving)
                _actor.broadcastPacket(new VehicleStarted(_actor.getObjectId(), 1));

            _client_moving = true;
            _accessor.moveTo(x, y, z);
            _actor.broadcastPacket(new VehicleDeparture(getActor()));
        }
    }

    @Override
    protected void clientStoppedMoving()
    {
        _client_moving = false;
        _actor.broadcastPacket(new VehicleStarted(_actor.getObjectId(), 0));
        _actor.broadcastPacket(new VehicleInfo(getActor()));
    }

    @Override
    protected void clientStopMoving(L2CharPosition pos)
    {
        if (_actor.isMoving())
            _accessor.stopMove(pos);

        if (_client_moving || pos != null)
        {
            _client_moving = false;
            _actor.broadcastPacket(new VehicleStarted(_actor.getObjectId(), 0));
            _actor.broadcastPacket(new VehicleInfo(getActor()));
        }
    }

    @Override
    public L2BoatInstance getActor()
    {
        return (L2BoatInstance) _actor;
    }
}