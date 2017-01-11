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
package net.sf.l2j.gameserver.model;

import java.util.List;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ExCloseMPCC;
import net.sf.l2j.gameserver.serverpackets.ExOpenMPCC;
import net.sf.l2j.gameserver.serverpackets.ServerBasePacket;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 *
 * @author  chris_00
 */
public class L2CommandChannel
{
    private List<L2Party> _parties = null;
    private L2PcInstance _commandLeader = null;

    /**
     * Creates a New Command Channel and Add the Leaders party to the CC
     * 
     * @param CommandChannelLeader
     *  
     */
    public L2CommandChannel(L2PcInstance leader) 
    {
        _commandLeader = leader;
        _parties = new FastList<>();
        _parties.add(leader.getParty());
        leader.getParty().setCommandChannel(this);
        leader.getParty().broadcastToPartyMembers(new ExOpenMPCC());
    }

    /**
     * Adds a Party to the Command Channel
     * @param Party
     */
    public void addParty(L2Party party)
    {
        _parties.add(party);
        party.setCommandChannel(this);

        if (_parties.size() == Config.ALT_CHANNEL_ACTIVATION_COUNT)
            broadcastToChannelMembers(new SystemMessage(SystemMessage.CHANNEL_ACTIVATED));

        party.broadcastToPartyMembers(new ExOpenMPCC());
    }

    /**
     * Removes a Party from the Command Channel
     * @param Party
     */
    public void removeParty(L2Party party)
    {
        _parties.remove(party);

        party.setCommandChannel(null);
        party.broadcastToPartyMembers(new ExCloseMPCC());

        if (_parties.size() < 2)
        {
            party.broadcastToPartyMembers(new SystemMessage(SystemMessage.COMMAND_CHANNEL_DISBANDED));
            broadcastToChannelMembers(new SystemMessage(SystemMessage.COMMAND_CHANNEL_DISBANDED));
            disbandChannel();
        }
        else if (_parties.size() < Config.ALT_CHANNEL_ACTIVATION_COUNT)
            broadcastToChannelMembers(new SystemMessage(SystemMessage.CHANNEL_DEACTIVATED));
    }

    /**
     * disbands the whole Command Channel 
     */
    public void disbandChannel()
    {
        if (_parties != null)
        {
            for (L2Party party : _parties)
            {
                if (party == null)
                    continue;

                removeParty(party);
            }
        }
        _parties = null;
    }

    /**
     * @return overall membercount of the Command Channel 
     */
    public int getMemberCount()
    {
        int count = 0;
        for (L2Party party : _parties)
        {
            if (party != null)
                count += party.getMemberCount();
        }
        return count;
    }

    /**
     * Broadcast packet to every channelmember 
     * @param ServerBasePacket
     */
    public void broadcastToChannelMembers(ServerBasePacket gsp) 
    {
        if (!_parties.isEmpty())
        {
            for (L2Party party : _parties)
            {
                if (party != null)
                    party.broadcastToPartyMembers(gsp);
            }
        }
    }

    /**
     * @return list of Parties in Command Channel  
     */
    public List<L2Party> getParties()
    {
        return _parties;
    }

    /**
     * @return list of all Members in Command Channel  
     */
    public List<L2PcInstance> getMembers()
    {
        List<L2PcInstance> members = new FastList<>();
        for (L2Party party : getParties())
            members.addAll(party.getPartyMembers());

        return members;
    }

    /**
     * @param sets the leader of the Command Channel 
     */
    public void setChannelLeader(L2PcInstance leader)
    {
        _commandLeader = leader;
    }

    /**
     * @return the leader of the Command Channel  
     */
    public L2PcInstance getChannelLeader()
    {
        return _commandLeader;
    }
}