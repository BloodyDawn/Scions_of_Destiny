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
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.PcFreight;
import net.sf.l2j.gameserver.serverpackets.PackageSendableList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Format: (c)d
 * d: char object id (?)
 * @author  -Wooden-
 */
public class RequestPackageSendableItemList extends ClientBasePacket
{
    private static final String _C_9E_REQUESTPACKAGESENDABLEITEMLIST = "[C] 9E RequestPackageSendableItemList";
    private int _objectID;

    public RequestPackageSendableItemList(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _objectID = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    public void runImpl()
    {
        if (getClient().getActiveChar() == null)
            return;

        L2ItemInstance[] items = getClient().getActiveChar().getInventory().getAvailableItems(true);

        getClient().getActiveChar().setActiveWarehouse(new PcFreight(null));

        // build list...
        sendPacket(new PackageSendableList(items, _objectID));
        sendPacket(new SystemMessage(SystemMessage.PACKAGES_CAN_ONLY_BE_RETRIEVED_HERE));
    }

    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C_9E_REQUESTPACKAGESENDABLEITEMLIST;
    }
}