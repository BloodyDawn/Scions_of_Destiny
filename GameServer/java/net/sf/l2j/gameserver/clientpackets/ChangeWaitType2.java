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
package net.sf.l2j.gameserver.clientpackets;

import java.nio.ByteBuffer;

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ChairSit;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class ChangeWaitType2 extends ClientBasePacket
{
    private static final String _C__1D_CHANGEWAITTYPE2 = "[C] 1D ChangeWaitType2";

    private final boolean _typeStand;

    /**
     * packet type id 0x1d
     * 
     * sample
     * 
     * 1d
     * 01 00 00 00 // type (0 = sit, 1 = stand)
     * 
     * format:		cd
     * @param decrypt
     */
    public ChangeWaitType2(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _typeStand = (readD() == 1);
    }

    @Override
    public void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (getClient() != null && player != null)
	{
            if (player.isOutOfControl())
            {
                player.sendPacket(new ActionFailed());
                return;
            }

            if (player.getMountType() != 0)
                return;

            L2Object target = player.getTarget();

            if (target != null && !player.isSitting() && !_typeStand
                    && target instanceof L2StaticObjectInstance
                    && ((L2StaticObjectInstance)target).getType() == 1
                    && CastleManager.getInstance().getCastle(target) != null
                    && player.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
            {
                ChairSit cs = new ChairSit(player,((L2StaticObjectInstance)target).getStaticObjectId());
                player.sitDown();
                player.broadcastPacket(cs);
            }

	    if (_typeStand)
	        player.standUp();
	    else
            {
                if (!player.isPendingSitting())
                {
                    if (player.isMoving())
                        player.setIsPendingSitting(true);
                    else
                    {
                        if (player.isAttackingDisabled() || player.isImmobilized() || player.isCastingNow())
                            return;

                        player.sitDown();
                    }
                }
            }
	}
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__1D_CHANGEWAITTYPE2;
    }
}