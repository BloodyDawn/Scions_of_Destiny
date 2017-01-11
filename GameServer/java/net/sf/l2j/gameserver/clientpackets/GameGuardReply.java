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
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author zabbix
 * Lets drink to code!
 * 
 * Unknown Packet:ca
 * 0000: 45 00 01 00 1e 37 a2 f5 00 00 00 00 00 00 00 00    E....7..........
 */

public class GameGuardReply extends ClientBasePacket
{
    private static final String _C__CA_GAMEGUARDREPLY = "[C] CA GameGuardReply";
    private static Logger _log = Logger.getLogger(GameGuardReply.class.getName());

    public GameGuardReply(ByteBuffer buf, ClientThread client)
    {
        super(buf,client);
    }

    @Override
    public void runImpl()
    {
        _log.warning("GameGuard reply by account " + getClient().getLoginName());
        getClient().setGameGuardOk(true);
    }

    public String getType()
    {
        return _C__CA_GAMEGUARDREPLY;
    }
}