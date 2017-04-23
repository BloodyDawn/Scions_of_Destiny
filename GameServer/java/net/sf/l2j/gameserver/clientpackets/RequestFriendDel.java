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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.Friend;
import net.sf.l2j.gameserver.serverpackets.FriendList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendDel extends L2GameClientPacket
{
	
	private static final String _C__61_REQUESTFRIENDDEL = "[C] 61 RequestFriendDel";
	private static Logger _log = Logger.getLogger(RequestFriendDel.class.getName());
	
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	public void runImpl()
	{
		SystemMessage sm;
		
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		// Check if target is friend and delete him from friends list
		Friend friend = activeChar.getFriend(_name);
		if (friend == null)
		{
			// Player is not in your friendlist
			sm = new SystemMessage(SystemMessage.S1_NOT_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		// Remove friend from friend list
		activeChar.getFriendList().remove(friend);
		
		activeChar.sendPacket(new FriendList(activeChar));
		
		L2PcInstance otherPlayer = L2World.getInstance().getPlayer(_name);
		if (otherPlayer != null)
		{
			Friend requestor = otherPlayer.getFriend(activeChar.getName());
			if (requestor != null)
			{
				otherPlayer.getFriendList().remove(requestor);
			}
			
			otherPlayer.sendPacket(new FriendList(otherPlayer));
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (char_id=? AND friend_id=?) OR (char_id=? AND friend_id=?)"))
		{
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, friend.getObjectId());
			statement.setInt(3, friend.getObjectId());
			statement.setInt(4, activeChar.getObjectId());
			statement.execute();
			
			// Player deleted from your friendlist
			sm = new SystemMessage(SystemMessage.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			sm = null;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not del friend objectid: ", e);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__61_REQUESTFRIENDDEL;
	}
}