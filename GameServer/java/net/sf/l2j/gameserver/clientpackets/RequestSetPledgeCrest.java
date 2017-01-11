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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.idfactory.BitSetIDFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSetPledgeCrest extends ClientBasePacket
{
	private static final String _C__53_REQUESTSETPLEDGECREST = "[C] 53 RequestSetPledgeCrest";
	static Logger _log = Logger.getLogger(RequestSetPledgeCrest.class.getName());

	private final int _length;
	private final byte[] _data;

	public RequestSetPledgeCrest(ByteBuffer buf, ClientThread client)
	{
            super(buf, client);
            _length = readD();
            _data = readB(_length);
	}

        @Override
	public void runImpl()
	{
            L2PcInstance activeChar = getClient().getActiveChar();
            if (activeChar == null)
                return;

            L2Clan clan = activeChar.getClan();
            if (clan == null)
                return;

            if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) != L2Clan.CP_CL_REGISTER_CREST)
            {
                activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED));
                return;
            }

            if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
            {
                activeChar.sendPacket(new SystemMessage(552));
                return;
            }

            CrestCache crestCache = CrestCache.getInstance();

            if (_length < 0)
            {
                activeChar.sendMessage("File Transfer Error.");
                return;
            }

            if (_length > 256)
            {
                activeChar.sendMessage("The clan crest file size is greater than 256 bytes.");
                return;
            }

            if (_length == 0 || _data.length == 0)
            {
                crestCache.removePledgeCrest(clan.getCrestId());

                clan.setHasCrest(false);
                activeChar.sendMessage("The clan crest has been deleted.");

                for (L2PcInstance member : clan.getOnlineMembers(0))
                    member.broadcastUserInfo();

                return;
            }

            if (clan.getLevel() < 3)
            {
                activeChar.sendPacket(new SystemMessage(SystemMessage.CLAN_LVL_3_NEEDED_TO_SET_CREST));
                return;
            }

            int newId = BitSetIDFactory.getInstance().getNextId();

            if (clan.hasCrest())
            	crestCache.removePledgeCrest(newId);

            if (!crestCache.savePledgeCrest(newId,_data))
            {
                _log.log(Level.INFO, "Error loading crest of clan:" + clan.getName());
                return;
            }

            try (Connection con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?"))
            {
                statement.setInt(1, newId);
                statement.setInt(2, clan.getClanId());
                statement.executeUpdate();
            }
            catch (SQLException e)
            {
                _log.warning("could not update the crest id:"+e.getMessage());
            }

            clan.setCrestId(newId);
            clan.setHasCrest(true);

            for (L2PcInstance member : clan.getOnlineMembers(0))
                member.broadcastUserInfo();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
            return _C__53_REQUESTSETPLEDGECREST;
	}
}