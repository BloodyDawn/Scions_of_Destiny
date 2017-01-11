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

public class RequestDeleteMacro extends ClientBasePacket
{  
	private int _id;
	
	private static final String _C__C2_REQUESTDELETEMACRO = "[C] C2 RequestDeleteMacro";
	
	/**
	 * packet type id 0xc2
	 * 
	 * sample
	 * 
	 * c2
	 * d // macro id
	 * 
	 * format:		cd
	 * @param decrypt
	 */
	public RequestDeleteMacro(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_id = readD();
	}

        @Override
	public void runImpl()
	{
                if (getClient().getActiveChar() == null)
                        return;

                getClient().getActiveChar().deleteMacro(_id);
	        getClient().getActiveChar().sendMessage("Delete macro id="+_id);
	}
	
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__C2_REQUESTDELETEMACRO;
	}
}