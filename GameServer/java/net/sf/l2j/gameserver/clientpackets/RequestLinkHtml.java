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
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class RequestLinkHtml extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(RequestLinkHtml.class.getName());
	private static final String REQUESTLINKHTML__C__20 = "[C] 20 RequestLinkHtml";
	private String _link;

	public RequestLinkHtml(ByteBuffer buf, ClientThread client)
	{
		super(buf,client);
	}

        @Override
	public void runImpl()
	{
		L2PcInstance actor = getClient().getActiveChar();
		if(actor == null)
			return;
		
		_link = readS();
		
		if(_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warning("[RequestLinkHtml] hack? link contains prohibited characters: '"+_link+"', skipped");
			return;
		}
		
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setFile(_link);
		
		sendPacket(msg);
	}
	
	public String getType()
	{
		return REQUESTLINKHTML__C__20;
	}
}
