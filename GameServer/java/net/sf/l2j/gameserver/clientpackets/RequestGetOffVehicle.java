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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.GetOffVehicle;
import net.sf.l2j.gameserver.serverpackets.StopMoveInVehicle;

/**
 * @author Maktakien
 */
public class RequestGetOffVehicle extends L2GameClientPacket
{
	private int _boatId, _x, _y, _z;
	
	@Override
	protected void readImpl()
	{
		_boatId = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!activeChar.isInBoat() || (activeChar.getBoat().getObjectId() != _boatId) || !activeChar.isInsideRadius(_x, _y, _z, 1000, true, false))
		{
			sendPacket(new ActionFailed());
			return;
		}
		
		if (activeChar.getBoat().isMoving())
		{
			activeChar.broadcastPacket(new StopMoveInVehicle(activeChar, _boatId));
		}
		
		activeChar.setBoat(null);
		activeChar.setInBoatPosition(null);
		sendPacket(new ActionFailed());
		activeChar.broadcastPacket(new GetOffVehicle(activeChar.getObjectId(), _boatId, _x, _y, _z));
		activeChar.setXYZ(_x, _y, _z);
		activeChar.revalidateZone(true);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "[S] 5d GetOffVehicle";
	}
}