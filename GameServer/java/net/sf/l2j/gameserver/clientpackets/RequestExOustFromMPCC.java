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

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * @author -Wooden-
 *
 */
public class RequestExOustFromMPCC extends ClientBasePacket
{
    private static final String _C__D0_0F_REQUESTEXOUSTFROMMPCC = "[C] D0:0F RequestExOustFromMPCC";
    private String _name;

    /**
     * @param buf
     * @param client
     */
    public RequestExOustFromMPCC(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _name = readS();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    public void runImpl()
    {
        L2PcInstance target = L2World.getInstance().getPlayer(_name);
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        if (target != null && target != activeChar && target.isInParty() && activeChar.isInParty() && activeChar.getParty().isInCommandChannel()
                && target.getParty().isInCommandChannel() && target.getParty().getCommandChannel() == activeChar.getParty().getCommandChannel()
                && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar))
        {
            target.getParty().getCommandChannel().removeParty(target.getParty());
            target.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_DISMISSED_FROM_CHANNEL));

            if (activeChar.getParty().isInCommandChannel())
            {
                SystemMessage sm = new SystemMessage(SystemMessage.S1_PARTY_DISMISSED_FROM_COMMAND_CHANNEL);
                sm.addString(target.getParty().getPartyMembers().get(0).getName());
                activeChar.getParty().getCommandChannel().broadcastToChannelMembers(sm);
            }
        }
        else
            activeChar.sendPacket(new SystemMessage(SystemMessage.INCORRECT_TARGET));
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_0F_REQUESTEXOUSTFROMMPCC;
    }
}