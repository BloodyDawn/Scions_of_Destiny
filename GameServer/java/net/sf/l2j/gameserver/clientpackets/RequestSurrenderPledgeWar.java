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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class RequestSurrenderPledgeWar extends ClientBasePacket
{
    private static final String _C__51_REQUESTSURRENDERPLEDGEWAR = "[C] 51 RequestSurrenderPledgeWar";
    private static Logger _log = Logger.getLogger(RequestSurrenderPledgeWar.class.getName());

    private String _pledgeName;

    public RequestSurrenderPledgeWar(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _pledgeName = readS();
    }

    @Override
    public void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
	if (player == null)

            return;

        L2Clan _clan = player.getClan();
	if (_clan == null)

            return;


        L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
        if (clan == null)
        {
            player.sendMessage("No such clan.");
            player.sendPacket(new ActionFailed());
            return;                        
        }

        if (Config.DEBUG)
            _log.info("RequestSurrenderPledgeWar by "+getClient().getActiveChar().getClan().getName()+" with "+_pledgeName);
        
        if (!_clan.isAtWarWith(clan.getClanId()))
        {
            player.sendMessage("You aren't at war with this clan.");
            player.sendPacket(new ActionFailed());
            return;            
        }

        SystemMessage msg = new SystemMessage(SystemMessage.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
        msg.addString(_pledgeName);
        player.sendPacket(msg);
        msg = null;
        player.deathPenalty(false);
        ClanTable.getInstance().deleteclanswars(_clan.getClanId(), clan.getClanId());
    }
    
    public String getType()
    {
        return _C__51_REQUESTSURRENDERPLEDGEWAR;
    }
}