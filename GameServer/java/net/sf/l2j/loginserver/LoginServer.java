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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.status.Status;
import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

/**
 * This class ...
 * 
 * @version $Revision: 1.9.4.4 $ $Date: 2005/03/27 15:30:09 $
 */
public class LoginServer extends FloodProtectedListener
{
    private static Logger _log = Logger.getLogger(LoginServer.class.getName());

    private static LoginServer _instance;

    private Status _statusServer;
    private GameServerListener _gslistener;

    private final ThreadPoolExecutor _generalPacketsExecutor;

    public static int PROTOCOL_REV = 0x0102;

    public static LoginServer getInstance()
    {
        if (_instance == null)
            _instance = new LoginServer();
        return _instance;
    }

    public static void main(String[] args)
    {
        Server.SERVER_MODE = Server.MODE_LOGINSERVER;

        // Load log folder first
        loadLogFolder();

        // Initialize config
        Config.load();

        LoginServer server = LoginServer.getInstance();
        server.start();

        _log.info("Login Server ready on " + Config.LOGIN_BIND_ADDRESS + ":" + Config.PORT_LOGIN);
    }

    public void shutdown(boolean restart)
    {
        interrupt();

        // shut down executor
        try
        {
            _generalPacketsExecutor.awaitTermination(1,TimeUnit.SECONDS);
            _generalPacketsExecutor.shutdown();
        }
        catch (Throwable t)
        {
        }

        _gslistener.interrupt();
        GameServerTable.getInstance().shutDown();

        close();

        if (restart)
            Runtime.getRuntime().exit(2);
        else
            Runtime.getRuntime().exit(0);
    }

    private LoginServer()
    {
        super(Config.LOGIN_BIND_ADDRESS, Config.PORT_LOGIN);

        // Prepare Database
        try
        {
            L2DatabaseFactory.getInstance();
        }
        catch (SQLException e)
        {
            _log.severe("FATAL: Failed initializing database. Reason: " + e.getMessage());
            if (Config.DEVELOPER)
                e.printStackTrace();
            System.exit(1);
        }

        LoginController.getInstance();
        GameServerTable.getInstance();

        // Load Ban file
        loadBanFile();

        // start packet executor
        _generalPacketsExecutor = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        _gslistener = GameServerListener.getInstance();
        _gslistener.start();

        if (Config.IS_TELNET_ENABLED)
        {
            try
            {
                _statusServer = new Status(Server.SERVER_MODE);
                _statusServer.start();
            }
            catch (IOException e)
            {
                _log.severe("Failed to start the Telnet Server. Reason: " + e.getMessage());
                if (Config.DEVELOPER)
                    e.printStackTrace();
            }
        }
        else
            System.out.println("Telnet server is currently disabled.");
    }

    private static void loadLogFolder()
    {
        // Local Constants
        final String LOG_FOLDER = "log"; // Name of folder for log file
        final String LOG_NAME   = "./log.cfg"; // Name of log file

        /*** Main ***/
        // Create log folder
        File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
        logFolder.mkdir();

        // Create input stream for log file -- or store file data into memory
        try (InputStream is = new FileInputStream(new File(LOG_NAME)))
        {
            LogManager.getLogManager().readConfiguration(is);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void loadBanFile()
    {
        File bannedFile = new File("./banned_ip.cfg");
        if (bannedFile.exists() && bannedFile.isFile())
        {
            String line;
            String[] parts;

            try (FileInputStream fis = new FileInputStream(bannedFile);
                InputStreamReader ir = new InputStreamReader(fis);
                LineNumberReader reader = new LineNumberReader(ir))
            {
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    // check if this line isnt a comment line
                    if ((line.length() > 0) && (line.charAt(0) != '#'))
                    {
                        // split comments if any
                        parts = line.split("#");

                        // discard comments in the line, if any
                        line = parts[0];

                        parts = line.split(" ");

                        String address = parts[0];

                        long duration = 0;

                        if (parts.length > 1)
                        {
                            try
                            {
                                duration = Long.parseLong(parts[1]);
                            }
                            catch (NumberFormatException e)
                            {
                                _log.warning("Skipped: Incorrect ban duration (" + parts[1] + ") on (" + bannedFile.getName() + "). Line: " + reader.getLineNumber());
                                continue;
                            }
                        }

                        LoginController.getInstance().addBannedIP(address, duration);
                    }
                }
            }
            catch (IOException e)
            {
                _log.warning("Error while reading the bans file (" + bannedFile.getName() + "). Details: " + e.getMessage());
                if (Config.DEVELOPER)
                    e.printStackTrace();
            }
            _log.config("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
        }
        else
            _log.config("IP Bans file (" + bannedFile.getName() + ") is missing or is a directory, skipped.");
    }

    public Status getStatusServer()
    {
        return _statusServer;
    }

    /**
     *
     */
    public boolean unblockIp(String ipAddress)
    {
        if (LoginController.getInstance().ipBlocked(ipAddress))
            return true;

        return false;
    }

    public static class ForeignConnection
    {
    	/**
         * @param l
         */
        public ForeignConnection(long time)
        {
            lastConnection = time;
            connectionNumber = 1;
        }

        public int connectionNumber;
        public long lastConnection;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.loginserver.FloodProtectedListener#addClient(java.net.Socket)
     */
    @Override
    public void addClient(Socket s)
    {
        try
        {
            new ClientThread(s);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void execute(ClientBasePacket packet)
    {
        try
        {
            _generalPacketsExecutor.execute(packet);
        }
        catch (RejectedExecutionException e)
        {
            // if the server is shutdown we ignore
            if (!_generalPacketsExecutor.isShutdown())
                _log.severe("Failed executing: "+packet.getClass().getSimpleName()+" for IP: " + packet.getClient().getSocket().getInetAddress().getHostAddress());
        }
    }
}