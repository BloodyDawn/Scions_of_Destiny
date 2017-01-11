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
package net.sf.l2j.loginserver.clientpackets;

import net.sf.l2j.loginserver.ClientThread;
import net.sf.l2j.loginserver.ClientThread.LoginClientState;
import net.sf.l2j.loginserver.serverpackets.GGAuth;

public class RequestAuthGG extends ClientBasePacket
{
    public RequestAuthGG(byte[] rawPacket, ClientThread client)
    {
        super(rawPacket, client);
    }

    @Override
    public void run()
    {
        getClient().setState(LoginClientState.AUTHED_GG);
        getClient().sendPacket(new GGAuth(GGAuth.SKIP_GG_AUTH_REQUEST));
    }
}