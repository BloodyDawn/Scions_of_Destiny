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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Siege;

/**
 * sample
 * 0b 
 * 952a1048     objectId 
 * 00000000 00000000 00000000 00000000 00000000 00000000
 *
 * format  dddddd   rev 377
 * format  ddddddd   rev 417
 * 
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/27 18:46:18 $
 */
public class Die extends ServerBasePacket
{
    private static final String _S__0B_DIE = "[S] 06 Die";
    private int _chaId;
    private boolean _fake;
    private boolean _inEvent;
    private boolean _sweepable;
    private int _access;
    private L2Clan _clan;
    L2Character _cha;

    /**
     * @param _characters
     */
    public Die(L2Character cha)
    {
        _cha = cha;
        if (cha instanceof L2PcInstance)
        {
            L2PcInstance player = (L2PcInstance)cha;
            _access = player.getAccessLevel();
            _clan=player.getClan();
            _inEvent = player.getEventTeam() > 0;
        }
        _chaId = cha.getObjectId();
        _fake = !cha.isDead();
        if (cha instanceof L2Attackable)
            _sweepable = ((L2Attackable)cha).isSweepActive();
    }

    final void writeImpl()
    {
        if (_fake || _inEvent)
            return;

        writeC(0x06);

        writeD(_chaId); 
        // NOTE:
        // 6d 00 00 00 00 - to nearest village
        // 6d 01 00 00 00 - to hide away
        // 6d 02 00 00 00 - to castle
        // 6d 03 00 00 00 - to siege HQ
        // sweepable
        // 6d 04 00 00 00 - FIXED

        writeD(0x01);                                                   // 6d 00 00 00 00 - to nearest village
        if (_clan != null)
        {
            boolean isAttackerWithFlag = false;
            boolean isDefender = false;
            Siege siege = SiegeManager.getInstance().getSiege(_cha);
            if (siege != null)
            {
                isAttackerWithFlag = siege.getAttackerClan(_clan) != null && siege.getAttackerClan(_clan).getFlag().size() > 0 && !siege.checkIsDefender(_clan);
                isDefender = siege.getAttackerClan(_clan) == null && siege.checkIsDefender(_clan);
            }

            writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00);                  // 6d 01 00 00 00 - to hide away
            writeD(_clan.getHasCastle() > 0 || isDefender ? 0x01 : 0x00);     // 6d 02 00 00 00 - to castle
            writeD(isAttackerWithFlag ? 0x01 : 0x00);                         // 6d 03 00 00 00 - to siege HQ
        }
        else 
        {
            writeD(0x00);                                               // 6d 01 00 00 00 - to hide away
            writeD(0x00);                                               // 6d 02 00 00 00 - to castle
            writeD(0x00);                                               // 6d 03 00 00 00 - to siege HQ
        }

        writeD(_sweepable ? 0x01 : 0x00);                               // sweepable  (blue glow)
        writeD(_access >= Config.GM_FIXED ? 0x01: 0x00);                  // 6d 04 00 00 00 - to FIXED
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__0B_DIE;
    }
}