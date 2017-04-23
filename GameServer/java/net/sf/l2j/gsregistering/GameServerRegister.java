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
package net.sf.l2j.gsregistering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.loginserver.GameServerTable;

public class GameServerRegister
{
	private static String _choice;

	public static void main(String[] args) throws IOException
	{
		Server.SERVER_MODE = Server.MODE_LOGINSERVER;
		Config.load();

		try
		{
			GameServerTable.load();
		}
		catch (Exception e)
		{
			System.out.println("FATAL: Failed loading GameServerTable. Reason: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		GameServerTable gameServerTable = GameServerTable.getInstance();
		System.out.println("Welcome to L2JLisvus GameServer Registration.");
		System.out.println("Enter The id of the server you want to register or type help to get a list of ids:");

		try (InputStreamReader ir = new InputStreamReader(System.in);
			LineNumberReader _in = new LineNumberReader(ir))
		{
			while (true)
			{
				System.out.println("Your choice:");
				_choice = _in.readLine();

				if (_choice.equalsIgnoreCase("help"))
				{
					for (Map.Entry<Integer, String> entry : gameServerTable.serverNames.entrySet())
					{
						System.out.println("Server: id:" + entry.getKey() + " - " + entry.getValue());
					}
					System.out.println("You can also see servername.xml");
				}
				else
				{
					try
					{
						int id = new Integer(_choice).intValue();
						if (id > gameServerTable.serverNames.size())
						{
							System.out.println("ID is too high (max is " + (gameServerTable.serverNames.size()) + ")");
							continue;
						}

						if (id < 1)
						{
							System.out.println("ID must be a number above 0.");
							continue;
						}
						
						if (gameServerTable.isIDfree(id))
						{
							byte[] hex = LoginServerThread.generateHex(16);
							gameServerTable.createServer(gameServerTable.new GameServer(hex, id));
							Config.saveHexid(new BigInteger(hex).toString(16), "hexid.txt");
							System.out.println("Server Registered hexid saved to 'hexid.txt'");
							System.out.println("Put this file in the /config folder of your gameserver.");
							System.exit(0);
						}
						else
						{
							System.out.println("This id is not free!");
						}
					}
					catch (NumberFormatException nfe)
					{
						System.out.println("Please, type a number or 'help'.");
					}
				}
			}
		}
	}
}