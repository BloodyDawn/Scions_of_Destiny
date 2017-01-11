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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Sdh(h dddhh [dhhh] d)
 * Sdh ddddd ddddd ddddd ddddd
 * @version $Revision: 1.1.2.1.2.4 $ $Date: 2005/03/29 23:15:10 $
 */
public class GMViewWarehouseWithdrawList extends ServerBasePacket
{
    private static final String _S__95_GMViewWarehouseWithdrawList = "[S] 95 GMViewWarehouseWithdrawList";
    private L2ItemInstance[] _items;
    private L2PcInstance _character;
    private int _money;

    public GMViewWarehouseWithdrawList(L2PcInstance cha)
    {
        _character = cha;
        _items = _character.getWarehouse().getItems();
        _money = cha.getAdena();
    }

    final void writeImpl()
    {
        writeC(0x95);
        writeS(_character.getName());
        writeD(_money);
        writeH(_items.length);

        for (L2ItemInstance item : _items)
        {
            writeH(item.getItem().getType1());
            writeD(item.getObjectId());
            writeD(item.getItemId());
            writeD(item.getCount());
            writeH(item.getItem().getType2());
            writeH(item.getCustomType1());

            if (item.isEquipable())
            {
                writeD(item.getItem().getBodyPart());
                writeH(item.getEnchantLevel());
                writeH(0x00);
                writeH(0x00);
            }

            writeD(item.getObjectId());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__95_GMViewWarehouseWithdrawList;
    }
}