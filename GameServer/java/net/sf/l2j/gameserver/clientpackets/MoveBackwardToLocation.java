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

import java.nio.BufferUnderflowException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 * @version $Revision: 1.11.2.4.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class MoveBackwardToLocation extends L2GameClientPacket
{
	// private static Logger _log = Logger.getLogger(MoveBackwardToLocation.class.getName());

	// cdddddd
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	@SuppressWarnings("unused")
	private int _originX;
	@SuppressWarnings("unused")
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	private int _moveMovement;

	// For geodata
	private int _CurX;
	private int _CurY;
	private int _CurZ;

	private static final String _C__01_MOVEBACKWARDTOLOC = "[C] 01 MoveBackwardToLoc";

	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		try
		{
			_moveMovement = readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (!Config.ALLOW_L2WALKER)
			{
				getClient().getActiveChar().sendPacket(new SystemMessage(769));
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), "Player " + getClient().getActiveChar().getName() + " is trying to use L2Walker and got kicked.", Config.DEFAULT_PUNISH);
			}
		}
	}

	@Override
	public void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		_targetZ += activeChar.getTemplate().collisionHeight;
		_CurX = activeChar.getX();
		_CurY = activeChar.getY();
		_CurZ = activeChar.getZ();

		if (activeChar.getTeleMode() > 0)
		{
			if (activeChar.getTeleMode() == 1)
			{
				activeChar.setTeleMode(0);
			}
			activeChar.sendPacket(new ActionFailed());
			activeChar.teleToLocation(_targetX, _targetY, _targetZ, false);
			return;
		}

		// Disable keyboard movement when geodata is not enabled and player is not flying.
		if ((_moveMovement == 0) && (Config.GEODATA < 1) && !activeChar.isFlying())
		{
			activeChar.sendPacket(new ActionFailed());
		}
		else
		{
			double dx = _targetX - _CurX;
			double dy = _targetY - _CurY;
			// Can't move if character is confused, or trying to move a huge distance
			if (activeChar.isOutOfControl() || (((dx * dx) + (dy * dy)) > 98010000)) // 9900*9900
			{
				activeChar.sendPacket(new ActionFailed());
				return;
			}

			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));
			
			if (activeChar.getParty() != null)
			{
				activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__01_MOVEBACKWARDTOLOC;
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return true;
	}
}