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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Support for "Chat with Friends" dialog. 
 * 
 * Format: ch (hdSdh)
 * h: Total Friend Count
 * 
 * h: Unknown
 * d: Player Object ID
 * S: Friend Name
 * d: Online/Offline
 * h: Unknown
 * 
 * @author Tempy
 *
 */
public class FriendList extends ServerBasePacket
{
    private static Logger _log = Logger.getLogger(FriendList.class.getName());
    private static final String _S__FA_FRIENDLIST = "[S] FA FriendList";

    private L2PcInstance _cha;

    public FriendList(L2PcInstance cha)
    {
        _cha = cha;
    }

    final void writeImpl()
    {
        L2PcInstance _activeChar = getClient().getActiveChar();
        if (_activeChar == null)
            return;

        writeC(0xfa);

        if (_activeChar.getFriendList().size() > 0)
        {
            writeH(_activeChar.getFriendList().size());

            for (L2PcInstance.Friend friend : _activeChar.getFriendList())
            {
                if (friend.getObjectId() == _cha.getObjectId())
                {
                    if (!friend.getName().equals(_cha.getName()))
                        friend.setName(_cha.getName());
                    friend.setOnline(_cha.isOnline());
                }

                writeH(0); // ??
                writeD(friend.getObjectId());
                writeS(friend.getName());

                writeD(friend.isOnline()); // online status

                writeH(0); // ??
            }				
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__FA_FRIENDLIST;
    }
}