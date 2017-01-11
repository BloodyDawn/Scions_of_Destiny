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

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;

/**
 * Format: (ch) dd [dddc]
 * @author l3x
 *
 */
public class RequestSetCrop extends ClientBasePacket
{
    private static final String _C__D0_0B_REQUESTSETCROP = "[C] D0:0B RequestSetCrop";

    private int _size;
    private int _manorId;
    private int[] _items; // _size*4

    /**
     * @param buf
     * @param client
     */
    public RequestSetCrop(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _manorId = readD();
        _size = readD();
        if (_size > 500)
        {
            _size = 0;
            return;
        }

        _items = new int[_size * 4];
        for (int i = 0; i < _size; i++)
        {
            int itemId = readD();
            _items[i * 4 + 0] = itemId;
            int sales = readD();
            _items[i * 4 + 1] = sales;
            int price = readD();
            _items[i * 4 + 2] = price;
            int type = readC();
            _items[i * 4 + 3] = type;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    public void runImpl()
    {
        if (_size < 1)
            return;

        FastList<CropProcure> crops = new FastList<>();
        for (int i = 0; i < _size; i++)
        {
            int id = _items[i * 4 + 0];
            int sales = _items[i * 4 + 1];
            int price = _items[i * 4 + 2];
            int type = _items[i * 4 + 3];
            if (id > 0)            
                crops.add(CastleManorManager.getInstance().getNewCropProcure(id, sales, type, price, sales));
        }

        CastleManager.getInstance().getCastleById(_manorId).setCropProcure(crops, CastleManorManager.PERIOD_NEXT);
        if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
            CastleManager.getInstance().getCastleById(_manorId).saveCropData(CastleManorManager.PERIOD_NEXT);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_0B_REQUESTSETCROP;
    }
}