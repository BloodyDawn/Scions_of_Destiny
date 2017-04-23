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
package net.sf.l2j.loginserver;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;

/**
 * This class waits for incoming connections from GameServers and launches (@link GameServerThread GameServerThreads}
 * @author luisantonioa, -Wooden-
 */

public class GameServerListener extends FloodProtectedListener
{
	protected static Logger _log = Logger.getLogger(LoginServer.class.getName());
	private final List<GameServerThread> _gameServerThreads = new FastList<>();
	
	public GameServerListener() throws IOException
	{
		super(Config.GAME_SERVER_LOGIN_HOST, Config.GAME_SERVER_LOGIN_PORT);
		setName(getClass().getSimpleName());
	}
	
	/**
	 * @return Returns the gameServerThreads.
	 */
	public List<GameServerThread> getGameServerThreads()
	{
		return _gameServerThreads;
	}
	
	/**
	 * Removes a GameServerThread from the list
	 */
	public void removeGameServer(GameServerThread gst)
	{
		_gameServerThreads.remove(gst);
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.loginserver.FloodProtectedListener#addClient(java.net.Socket)
	 */
	@Override
	public void addClient(Socket s)
	{
		GameServerThread gst = new GameServerThread(s);
		_gameServerThreads.add(gst);
	}
}