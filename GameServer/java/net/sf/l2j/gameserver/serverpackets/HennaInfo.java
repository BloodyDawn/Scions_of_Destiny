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

import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class HennaInfo extends ServerBasePacket
{
    private static final String _S__E4_HennaInfo = "[S] E4 HennaInfo";

    private L2PcInstance _player;
    private L2HennaInstance[] _hennas = new L2HennaInstance[3];
    private int _count;

    public HennaInfo(L2PcInstance player)
    {
        _player = player;

        int j = 0;
        for (int i = 0; i < 3; i++)
        {
            L2HennaInstance h = _player.getHenna(i+1);
            if (h != null)
                _hennas[j++] = h;
        }
        _count = j;
    }

    final void writeImpl()
    {	
        writeC(0xe4);

        writeC(_player.getHennaStatINT());    //equip INT
        writeC(_player.getHennaStatSTR());    //equip STR
        writeC(_player.getHennaStatCON());    //equip CON
        writeC(_player.getHennaStatMEN());    //equip MEN
        writeC(_player.getHennaStatDEX());    //equip DEX
        writeC(_player.getHennaStatWIT());    //equip WIT

        writeD(3); // slots?

        writeD(_count); //size
        for (int i=0; i < _count; i++)
        {
            writeD(_hennas[i].getSymbolId());
            writeD(_hennas[i].getSymbolId());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__E4_HennaInfo;
    }
}