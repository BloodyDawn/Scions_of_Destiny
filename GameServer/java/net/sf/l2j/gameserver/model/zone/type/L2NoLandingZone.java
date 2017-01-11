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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * A no landing zone
 *
 * @author  durgus
 */
public class L2NoLandingZone extends L2ZoneType
{
    public L2NoLandingZone(int id)
    {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            character.setInsideZone(L2Character.ZONE_NOLANDING, true);
            if (((L2PcInstance) character).getMountType() == 2)
            {
                character.sendPacket(new SystemMessage(SystemMessage.AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_WYVERN));
                ((L2PcInstance)character).enteredNoLanding();
            }
        }
    }

    @Override
    protected void onExit(L2Character character)
    {
        if (character instanceof L2PcInstance)
        {
            character.setInsideZone(L2Character.ZONE_NOLANDING, false);
            if (((L2PcInstance) character).getMountType() == 2)
                ((L2PcInstance) character).exitedNoLanding();
        }
    }
}