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

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellEntry;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellIngredient;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellListContainer;

/**
 * This class ...
 * 
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class MultiSellList extends ServerBasePacket
{
    private static final String _S__D0_MULTISELLLIST = "[S] D0 MultiSellList";

    protected int _page, _finished;
    protected MultiSellListContainer _list;

    public MultiSellList(MultiSellListContainer list, int page, int finished)
    {
        _list = list;
        _page = page;
        _finished = finished;
    }

    final void writeImpl()
    {
    	// [ddddd] [dchh] [hdhdh] [hhdh]

        writeC(0xd0);
        writeD(_list.getListId());    // list id
        writeD(_page);		     // page
        writeD(_finished);	     // finished
        writeD(0x28);	             // size of pages
        writeD(_list == null ? 0 : _list.getEntries().size()); //list length

        if(_list != null)
        {
            for(MultiSellEntry ent : _list.getEntries())
            {
            	writeD(ent.getEntryId());
            	writeC(1);
            	writeH(ent.getProducts().size());
            	writeH(ent.getIngredients().size());

            	for (MultiSellIngredient i: ent.getProducts())
            	{
	            writeH(i.getItemId());
	            writeD(ItemTable.getInstance().getTemplate(i.getItemId()).getBodyPart());
	            writeH(ItemTable.getInstance().getTemplate(i.getItemId()).getType2());
	            writeD((int)i.getItemCount());
	            writeH(i.getEnchantmentLevel()); //enchant lvl
            	}

                for (MultiSellIngredient i : ent.getIngredients())
                {
                    int typeE = ItemTable.getInstance().getTemplate(i.getItemId()).getType2();
                    writeH(i.getItemId());      //ID
                    writeH(typeE);
                    writeD((int)i.getItemCount());	//Count
                    writeH(i.getEnchantmentLevel()); //Enchant Level
                }
            }
        }
    }

    @Override
    public String getType()
    {
        return _S__D0_MULTISELLLIST;
    }
}