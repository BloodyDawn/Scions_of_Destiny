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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.zone.L2ZoneSpawn;
import net.sf.l2j.gameserver.serverpackets.ClanHallDecoration;

/**
 * A clan hall zone
 *
 * @author  durgus
 */
public class L2ClanHallZone extends L2ZoneSpawn
{
    private int _clanHallId;

    public L2ClanHallZone(int id)
    {
        super(id);
    }

    @Override
    public void setParameter(String name, String value)
    {
        if (name.equals("clanHallId"))
        {
            _clanHallId = Integer.parseInt(value);

            // Register self to the correct clan hall
            ClanHallManager.getInstance().getClanHallById(_clanHallId).setZone(this);
        }
        else
            super.setParameter(name, value);
    }

    @Override
    protected void onEnter(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            // Set as in clan hall
            character.setInsideZone(L2Character.ZONE_CLANHALL, true);

            ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_clanHallId);
            if (clanHall == null)
                return;

            ((L2PcInstance)character).sendPacket(new ClanHallDecoration(clanHall));
        }
    }

    @Override
    protected void onExit(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            // Unset clanhall zone
            character.setInsideZone(L2Character.ZONE_CLANHALL, false);
        }
    }

    /**
     * Removes all foreigners from the clan hall
     * @param owningClanId
     */
    public void banishForeigners(int owningClanId)
    {
        for (L2Character temp : _characterList.values())
        {
            if (!(temp instanceof L2PcInstance))
                continue;

            if (((L2PcInstance)temp).getClanId() == owningClanId)
                continue;

            ((L2PcInstance)temp).teleToLocation(MapRegionTable.TeleportWhereType.Town); 
        }
    }
}