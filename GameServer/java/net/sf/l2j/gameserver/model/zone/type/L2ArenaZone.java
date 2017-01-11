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
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.zone.L2ZoneSpawn;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * An arena
 *
 * @author  durgus
 */
public class L2ArenaZone extends L2ZoneSpawn
{
    private String _arenaName;

    public L2ArenaZone(int id)
    {
        super(id);
    }

    @Override
    public void setParameter(String name, String value)
    {
        if (name.equals("name"))
            _arenaName = value;
        else
            super.setParameter(name, value);
    }

    @Override
    protected void onEnter(L2Character character)
    {
        character.setInsideZone(L2Character.ZONE_PVP, true);

        if (character instanceof L2PcInstance)
        {
            L2PcInstance player = (L2PcInstance)character;

            // Coliseum TvT Event restrictions
            if (getId() == 11012 && TvTEvent.getEventState() == TvTEvent.STARTED)
            {
                if (player.getEventTeam() == 0 && !player.isGM())
                    player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            }
            else
                player.sendPacket(new SystemMessage(SystemMessage.ENTERED_COMBAT_ZONE));
        }
    }

    @Override
    protected void onExit(L2Character character)
    {
        character.setInsideZone(L2Character.ZONE_PVP, false);

        if (character instanceof L2PcInstance)
            ((L2PcInstance)character).sendPacket(new SystemMessage(SystemMessage.LEFT_COMBAT_ZONE));
    }
}