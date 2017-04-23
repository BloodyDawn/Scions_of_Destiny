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

import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class RequestTutorialLinkHtml extends L2GameClientPacket
{
	private static final String _C__7b_REQUESTTUTORIALLINKHTML = "[C] 7b RequestTutorialLinkHtml";
	private String _bypass;
	
	@Override
	protected void readImpl()
	{
		_bypass = readS();
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		L2ClassMasterInstance.onTutorialLink(player, _bypass);
		
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent(_bypass, null, player);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__7b_REQUESTTUTORIALLINKHTML;
	}
}