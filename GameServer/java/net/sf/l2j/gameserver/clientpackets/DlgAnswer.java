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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * @author Dezmond_snz Format: cddd
 */
public class DlgAnswer extends L2GameClientPacket
{
	private static final String _C__C5_DLGANSWER = "[C] C5 DlgAnswer";
	private static Logger _log = Logger.getLogger(DlgAnswer.class.getName());

	private int _messageId;
	private int _answer, _unk;

	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_unk = readD();
	}
	
	@Override
	public void runImpl()
	{
		if (Config.DEBUG)
		{
			_log.fine(getType() + ": Answer acepted. Message ID " + _messageId + ", asnwer " + _answer + ", unknown field " + _unk);
		}
		
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (_messageId == SystemMessage.RESSURECTION_REQUEST)
		{
			activeChar.ReviveAnswer(_answer);
		}
		else if (_messageId == 1140)
		{
			activeChar.gatesAnswer(_answer, 1);
		}
		else if (_messageId == 1141)
		{
			activeChar.gatesAnswer(_answer, 0);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__C5_DLGANSWER;
	}
}
