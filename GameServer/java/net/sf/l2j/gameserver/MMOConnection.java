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
package net.sf.l2j.gameserver;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread.GameClientState;
import net.sf.l2j.gameserver.clientpackets.ClientBasePacket;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.serverpackets.ServerBasePacket;
import net.sf.l2j.gameserver.serverpackets.WrappedMessage;
import net.sf.l2j.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.8.4.9 $ $Date: 2005/04/06 16:13:38 $
 */
public final class MMOConnection
{
    private static Logger _log = Logger.getLogger(MMOConnection.class.getName());

    private Crypt _crypt;

    private final Socket _csocket;
    private final SocketChannel _channel;

    // Data for NIO, non-blocking IO
    /** Used by network to store the very first byte from incoming message */
    byte readBufferFirstByte;
    /** Used by network to store read buffer for uncomplete messages */
    ByteBuffer readBuffer;
    /** Used by network to store read buffer for uncomplete writes */
    ByteBuffer writeBuffer;
    /** Queue of received messages */
    final ConcurrentLinkedQueue<ClientBasePacket> _receivedMsgQueue;
    /** Queue of messages to be sent, if writeBuffer is not null */
    final BasePacketQueue _sendMsgQueue;
    /** This is a protection timestamp, for last time of writing into the
     * network buffer, if it's full. The connection will be closed if
     * we cannot write nothing for 30 seconds
     */
    int writeTimeStamp;
    /** The client that owns this connection */
    final ClientThread _client;

    private final byte[] _cryptkey =
    {
        (byte)0x94, (byte)0x35, (byte)0x00, (byte)0x00, 
        (byte)0xa1, (byte)0x6c, (byte)0x54, (byte)0x87   // these 4 bytes are fixed
    };

    public MMOConnection(ClientThread client, Socket socket)
    {
        _client = client;
        _csocket = socket;
        _channel = _csocket.getChannel();
        _receivedMsgQueue = new ConcurrentLinkedQueue<ClientBasePacket>();
        _sendMsgQueue = new BasePacketQueue();
        _crypt = new Crypt();
    }

    public ClientThread getClient()
    {
        return _client;
    }

    /**
     * Return IP adress of this Client Connection.
     */
    public String getIP()
    {
        if (!_csocket.isConnected())
            return null;

        return _csocket.getInetAddress().getHostAddress();
    }

    /** Put a message received by NIO's thread
     * Notifies all threads, that wait() on this Connection
     */
    public synchronized void addReceivedMsg(ByteBuffer buf)
    {
        if (Config.ASSERT) assert Thread.currentThread() == SelectorThread.getInstance();
        ClientBasePacket pkt = PacketHandler.handlePacket(buf, _client);

        try
        {
            if (pkt != null)
            {
                if (pkt.getClient().getState() == GameClientState.IN_GAME)
                    ThreadPoolManager.getInstance().executePacket(pkt);
                else
                    ThreadPoolManager.getInstance().executeIOPacket(pkt);
            }
        }
        catch (RejectedExecutionException e)
        {
            // if the server is shutdown we ignore
            if (!ThreadPoolManager.getInstance().isShutdown())
                _log.severe("Failed executing: "+pkt.getClass().getSimpleName()+" for Client: "+pkt.getClient().toString());
        }
    }

    /** Get next message, received received by NIO's thread, if any.
     * @return next message, or null if queue is empty.
     */
    private synchronized ClientBasePacket getNextReceivedMsg()
    {
        if (Config.ASSERT) assert Thread.currentThread() != SelectorThread.getInstance();
        if (_receivedMsgQueue.isEmpty())
            return null;

        return _receivedMsgQueue.remove();
    }

    /**
     * This method will be called indirectly by several threads, to notify
     * one client about all parallel events in the world.
     * it has to be either synchronized like this, or it might be changed to 
     * stack packets in a outbound queue. 
     * advantage would be that the calling thread is independent of the amount
     * of events that the target gets.
     * if one target receives hundreds of events in parallel, all event sources
     * will have to wait until the packets are send... 
     * for now, we use the direct communication
     * @param data
     * @throws IOException
     * @deprecated
     */
    @Deprecated
    public void sendPacket(byte[] data)
    {
        // this is time consuming.. only enable for debugging
        if (Config.DEBUG && _log.isLoggable(Level.FINEST))
            _log.finest("\n" + Util.printData(data));

        SelectorThread.getInstance().sendMessage(new WrappedMessage(data, this));
    }

    public void sendPacket(ServerBasePacket bp)
    {
        bp = (ServerBasePacket)bp.setConnection(this);
        SelectorThread.getInstance().sendMessage(bp);
    }

    public void activateCryptKey()
    {
        _crypt.setKey(_cryptkey);
    }

    public void decrypt(ByteBuffer b, int size)
    {
        _crypt.decrypt(b.array(), b.position(), size);
    }

    public void encrypt(ByteBuffer b)
    {
        _crypt.encrypt(b);
    }

    /**
     * this only gives the correct result if the cryptkey is not yet activated
     */
    public byte[] getCryptKey()
    {
        return _cryptkey;
    }

    public void onClientClose()
    {
        boolean fast = true;

        L2PcInstance player = getClient().getActiveChar();
        if (player != null)
        {
            player.setConnected(false);

            if (!Olympiad.getInstance().isRegisteredInComp(player)
                    && !player.isFestivalParticipant()
                    && !player.isInJail()
                    && player.getEventTeam() == 0
                    && !TvTEvent.isRegistered(player)
                    && !player.isInBoat())
            {
                if ((Config.OFFLINE_TRADE_ENABLE
                        && (player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL
                        || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL
                        || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY))
                        || (Config.OFFLINE_CRAFT_ENABLE && (player.isInCraftMode()
                        || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)))
                {
                    player.setInOfflineMode();

                    if (Config.OFFLINE_SET_NAME_COLOR)
                    {
                        player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
                        player.broadcastUserInfo();
                    }

                    if (player.getOfflineStartTime() == 0)
                        player.setOfflineStartTime(System.currentTimeMillis());

                    // Close connection socket
                    closeSocket();
                    return;
                }
            }

            // Check if player is fighting or changing class
            if (player.isInCombat() || player.isLocked())
                fast = false;
        }

        // Close connection socket
        closeSocket();

        ThreadPoolManager.getInstance().scheduleGeneral(new DisconnectionTask(), fast ? 5 : 15000);
    }

    /**
     * This will close the Connection And take care of everything that should
     * be done on disconnection (onDisconnect()) if the active char is not nulled yet
     */
    public void close(boolean socket)
    {
        if (socket)
            closeSocket();

        try
        {
            if (_client.getActiveChar() != null)
                _client.onDisconnect();

            if (_client.getLoginName() != null)
                LoginServerThread.getInstance().sendLogout(_client.getLoginName());
        }
        catch (Exception e)
        {
            _log.log(Level.WARNING, "", e);
        }
    }

    /**
     * This will close the connection socket
     */
    public void closeSocket()
    {
        try
        {
            if (_csocket != null && isSocketOpen())
                _csocket.close();
        }
        catch (IOException e) {}
    }
	
    /**
     * 
     */
    public SocketChannel getChannel()
    {
        return _channel;
    }

    /**
     * @return
     */
    public boolean isSocketOpen()
    {
        return !_csocket.isClosed();
    }

    private class DisconnectionTask implements Runnable
    {
        public void run()
        {
            close(false);
        }
    }
}