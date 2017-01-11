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
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.AskJoinPledge;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestJoinPledge extends ClientBasePacket
{
    private static final String _C__24_REQUESTJOINPLEDGE = "[C] 24 RequestJoinPledge";
    static Logger _log = Logger.getLogger(RequestJoinPledge.class.getName());

    private final int _target;

    public RequestJoinPledge(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        _target = readD();
    }

    @Override
    public void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null)
            return;

        if (!(L2World.getInstance().findObject(_target) instanceof L2PcInstance))
            return;

        L2PcInstance newMember = (L2PcInstance) L2World.getInstance().findObject(_target);

        if (!activeChar.getClan().CheckClanJoinCondition(activeChar, newMember))
            return;

        if (!activeChar.getRequest().setRequest(newMember, this))
            return;

        newMember.sendPacket(new AskJoinPledge(activeChar.getObjectId(), activeChar.getClan().getName()));
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__24_REQUESTJOINPLEDGE;
    }
}