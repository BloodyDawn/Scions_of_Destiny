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
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.TradeOtherAdd;
import net.sf.l2j.gameserver.serverpackets.TradeOwnAdd;
import net.sf.l2j.gameserver.serverpackets.TradeUpdate;

/**
 * This class ...
 * @version $Revision: 1.5.2.2.2.5 $ $Date: 2005/03/27 15:29:29 $
 */
public class AddTradeItem extends L2GameClientPacket
{
	private static final String _C__16_ADDTRADEITEM = "[C] 16 AddTradeItem";
	private static Logger _log = Logger.getLogger(AddTradeItem.class.getName());

	@SuppressWarnings("unused")
	private int _tradeId;
	private int _objectId;
	private int _count;

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readD();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}

		TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			_log.warning("Character: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
			return;
		}

		L2PcInstance partner = trade.getPartner();
		if ((partner == null) || (L2World.getInstance().findObject(partner.getObjectId()) == null) || (partner.getActiveTradeList() == null))
		{
			// Trade partner not found, cancel trade
			if (trade.getPartner() != null)
			{
				_log.warning("Character:" + player.getName() + " requested invalid trade object: " + _objectId);
			}

			player.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_NOT_FOUND_IN_THE_GAME));
			player.cancelActiveTrade();

			return;
		}

		if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed())
		{
			player.sendPacket(new SystemMessage(122));
			return;
		}

		if (Config.GM_DISABLE_TRANSACTION && (player.getAccessLevel() >= Config.GM_TRANSACTION_MIN) && (player.getAccessLevel() <= Config.GM_TRANSACTION_MAX))
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.cancelActiveTrade();
			return;
		}

		if (!player.validateItemManipulation(_objectId, "trade"))
		{
			player.sendPacket(new SystemMessage(SystemMessage.NOTHING_HAPPENED));
			return;
		}

		TradeList.TradeItem item = trade.addItem(_objectId, _count);
		if (item != null)
		{
			// Trade start packet updates tradelist here
			player.sendPacket(new TradeOwnAdd(item));
			player.sendPacket(new TradeUpdate(player));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__16_ADDTRADEITEM;
	}
}
