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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.clientpackets.ClientBasePacket;
import net.sf.l2j.gameserver.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.serverpackets.ServerBasePacket;
import net.sf.l2j.util.IPv4;

/**
 * NIO Selector thread.
 * Reads and writes network data.
 * All network messages are encoded and decoded withing this thread.
 * You may send messages using sendMessage() - it push the message
 * into internal queue and return immediatly.
 * A scheduler may read messages using recvMessage(), which returns
 * null when no more messaged left in internal queue.
 * 
 * Implementation description:
 * 1. Messages to sent are pushed into outbound queue by external threads.
 * 2. Received messages are pushed into inbound queue by this thread.
 * 3. The loop of Selector is:
 * - try to write messages from outbound queue
 * - if a message packet is not fully written - store write buffer in the client object
 * - wait for read/write/accept
 * - write data from buffers stored in client objects
 * - read data into a shared read buffer, parse packet and put messages to inbound queue
 * - if a message is not fully received - create a buffer of required
 * size and copy partially read date into it, store this new buffer in the client
 * 
 * TODO: Manage thread's priority. The thread has higher then normal
 * priority to handle network transfers quickly. But wakeup of this thread
 * for each small packet may be time-consuming.
 * 
 * @author Maxim Kizub
 */
@SuppressWarnings("rawtypes")
public final class SelectorThread extends Thread
{
    private static Logger _log = Logger.getLogger(SelectorThread.class.getName());

    private static SelectorThread _instance;

    /** Amount of buffers for writing data */
    private final int WRITE_BUF_HASH_SIZE = 4000;
    /** A size of each write buffer */
    private final int WRITE_BUF_SIZE = 128;
    /** A size of shared read/write buffer */
    private final int SHARED_BUF_SIZE = 65536; // 64*1024 = 65536

    /** Stack of write buffers */
    private final ByteBuffer[] writeBuffers;
    private int numWriteBuffers;
    /** Shared write buffer */
    private final ByteBuffer sharedWriteBuffer;
    /** Shared read buffer */
    private final ByteBuffer sharedReadBuffer;

    /** Outbound message queue */
    private BasePacketQueue sendMsgQueue;
    /** Inbound message queue */
    private BasePacketQueue recvMsgQueue;

    /** The selector */
    private Selector _selector;

    private IPv4 _ipv4;

    /** push counter, currently counts messages in putbound queue,
     * but better it count size of outbound queue...
     */
    private int msgCounter;

    private boolean _shutdown;

    public static SelectorThread getInstance()
    {
        if (_instance == null)
        {
            _instance = new SelectorThread();
        }
        return _instance;
    }

    private SelectorThread()
    {
        super("NIO Selector");
        if (Config.ASSERT)
            assert _instance == null;

        setPriority(Thread.NORM_PRIORITY+2);
        // write buffers (HeapByteBuffer is much faster for our purposes)
        writeBuffers = new ByteBuffer[WRITE_BUF_HASH_SIZE];
        for (int i=0; i < WRITE_BUF_HASH_SIZE; i++)
        {
            writeBuffers[i] = ByteBuffer.allocate(WRITE_BUF_SIZE);
            writeBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
            writeBuffers[i].clear();
            if (Config.ASSERT)
                assert writeBuffers[i].capacity() == WRITE_BUF_SIZE;
        }

        numWriteBuffers = WRITE_BUF_HASH_SIZE;
        // shared buffers (HeapByteBuffer is much faster for our purposes)
        sharedWriteBuffer = ByteBuffer.allocate(SHARED_BUF_SIZE);
        sharedReadBuffer  = ByteBuffer.allocate(SHARED_BUF_SIZE);
        sharedWriteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        sharedReadBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // queues
        sendMsgQueue = new BasePacketQueue();
        recvMsgQueue = new BasePacketQueue();

        _ipv4 = new IPv4();
    }

    /** Allocate a write buffer. Take it from a cashed stack,
     * of allocate a new one, if stack is empty or message is huge.
     * 
     * @param sz minimal size of the buffer
     * @return a read buffer, never null
     */
    private ByteBuffer allocateBuffer(int sz)
    {
        if (Config.ASSERT) assert Thread.currentThread() == this;
        if (sz <= WRITE_BUF_SIZE && numWriteBuffers > 0)
        {
            ByteBuffer b = writeBuffers[--numWriteBuffers]; 
            if (Config.ASSERT) assert b.position() == 0;
            if (Config.ASSERT) assert b.capacity() == WRITE_BUF_SIZE;
            if (Config.ASSERT) assert b.limit() >= sz;
            return b;
        }

        // (HeapByteBuffer is much faster for our purposes)
        ByteBuffer b = ByteBuffer.allocate(sz);
        b.order(ByteOrder.LITTLE_ENDIAN);
        if (Config.ASSERT) assert b.position() == 0;
        if (Config.ASSERT) assert b.limit() == sz;
        if (Config.ASSERT) assert b.capacity() == sz;
        return b;
    }

    /** Release a write buffer, store buffers to cashe stack, or just
     * forget it (to be collected by GC)
     * 
     * @param b the freed buffer
     */
    private void releaseBuffer(ByteBuffer b)
    {
        if (Config.ASSERT) assert Thread.currentThread() == this;
        if (Config.ASSERT) assert b != null;
        if (b.capacity() == WRITE_BUF_SIZE && numWriteBuffers <= WRITE_BUF_HASH_SIZE-1)
        {
            b.clear();
            if (Config.ASSERT) assert b.position() == 0;
            if (Config.ASSERT) assert b.limit() == WRITE_BUF_SIZE;
            writeBuffers[numWriteBuffers++] = b;
        }
    }

    public final void openServerSocket()
    {
        InetAddress address = null;

        if (!Config.GAMESERVER_HOSTNAME.equals("*"))
        {
            try
            {
                address = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
            }
            catch (UnknownHostException e1)
            {
                _log.log(Level.SEVERE, "WARNING: The GameServer bind address is invalid, using all avaliable IPs. Reason: " + e1.getMessage(), e1);
            }
        }

        try
        {
            // create selector
            _selector = Selector.open();

            // create a test server socket channel
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);

            ServerSocket ss = ssc.socket();

            if (address == null)
                ss.bind(new InetSocketAddress(Config.PORT_GAME));
            else
                ss.bind(new InetSocketAddress(address, Config.PORT_GAME));

            ssc.register(_selector, SelectionKey.OP_ACCEPT);

            // Start Selector
            start();

            _log.log(Level.INFO, "GameServer is now listening to " + Config.GAMESERVER_HOSTNAME + ":" + Config.PORT_GAME);
        }
        catch (IOException e)
        {
            _log.log(Level.SEVERE, getClass().getSimpleName() + "FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /** Main loop, see class description */
    public final void run()
    {
        while (!_shutdown)
        {
            // check if we have messages to pack and send
            processOutboudQueue();

            // reset counter
            if (msgCounter == 0)
                setPriority(Thread.NORM_PRIORITY+2); // idle
            else
                msgCounter = 0;

            // wait for read/write, timeout to be on safe side,
            // if waking up selector after a message push will fail
            // this is needed because we are in an infinite loop
            int numKeys = 0;
            try
            {
                numKeys = _selector.select(50L);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // skip if we don't have any keys to process
            if (numKeys > 0)
            {
                Iterator<SelectionKey> it = _selector.selectedKeys().iterator();
                // iterate over selected keys
                while (it.hasNext())
                {
                    SelectionKey sk = it.next();
                    it.remove();	

                    if (!sk.isValid() || sk == null)
                        continue;

                    MMOConnection con = (MMOConnection)sk.attachment();

                    switch (sk.readyOps())
                    {
                        case SelectionKey.OP_WRITE:
                            writeData(sk, con);
                            break;
                        case SelectionKey.OP_READ:
                            readData(sk, con);
                            break;
                        case SelectionKey.OP_WRITE | SelectionKey.OP_READ:
                            writeData(sk, con);
                            readData(sk, con);
                            break;
                        case SelectionKey.OP_ACCEPT:
                            acceptConnection(sk);
                            break;
                        case SelectionKey.OP_CONNECT:
                            finishConnection(sk, con);
                            break;
                    }
                }
            }

            // sleep some time to prevent thread from eating all cpu time
            try
            {
                sleep(30);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        closeSelectorThread();
    }

    public final void shutdown()
    {
        _shutdown = true;
    }

    private void closeSelectorThread()
    {
        for (final SelectionKey key : _selector.keys())
        {
            try
            {
                key.channel().close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }

        try
        {
            _selector.close();
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    /**
     * Write data to channel.
     * Writes only buffers stored in clients, i.e. data that was not
     * written completely.
     * If everything is written - disable interest on writing and
     * release write buffer to cache of buffers.
     *  
     * @param sk
     */
    private void writeData(SelectionKey sk, MMOConnection con)
    {
        try
        {
            if (!sk.isValid())
            {
                closeClient(sk, con);
                return;
            }

            final ByteBuffer b = con.writeBuffer;
            if (b != null)
            {
                int r = -1;

                try
                {
                    r = ((SocketChannel)sk.channel()).write(b);
                }
                catch (IOException e)
                {
                    _log.info("Error on writing data, " + con.getClient().toString() + " disconnected abnormally!");
                }

                if (r < 0)
                {
                    closeClient(sk, con);
                    return;
                }

                if (r > 0)
                {
                    con.writeTimeStamp = GameTimeController.getGameTicks();
                }
                else if (GameTimeController.getGameTicks() - con.writeTimeStamp > 300)
                {
                    // will release buffers
                    closeClient(sk, con);
                    return;
                }

                if (b.hasRemaining())
                    return;

                con.writeBuffer = null;
                releaseBuffer(b);
            }

            sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
        }
        catch (Throwable t)
        {
            _log.log(Level.INFO, "", t);
            closeClient(sk, con);
        }
    }

    /**
     * Read data from channel.
     * If there is an unfinished message (a read buffer stored on client),
     * then read it. Otherwice read into a shared read buffer.
     * If there is enough data - parses it and creates a message.
     *
     * If only one byte of a new message available - store it into
     * client's object.
     * If there are two or more bytes - then we know the size of
     * message, and can allocate a read buffer for it, copy
     * data and store to client's object.
     *
     * @param sk
     */
    private void readData(SelectionKey sk, MMOConnection con)
    {
        try
        {
            if (!sk.isValid() || !con.getChannel().isOpen())
            {
                closeClient(sk, con);
                return;
            }

            final ByteBuffer b;
            if (con.readBuffer != null)
            {
                b = con.readBuffer;
                if (Config.ASSERT) assert b.position() >= 2;
            }
            else
            {
                b = sharedReadBuffer;
                b.clear();
                if (Config.ASSERT) assert b.position() == 0;
                byte fb = con.readBufferFirstByte;
                if (fb != 0)
                {
                    b.put(fb);
                    con.readBufferFirstByte = 0;
                }
            }

            // if we try to to do a read with no space in the buffer it will
            // read 0 bytes
            // going into infinite loop
            if (b.position() == b.limit())
                System.exit(0);

            int r = -1;

            try
            {
                // read into shared/allocated buffer
                r = ((SocketChannel)sk.channel()).read(b);
            }
            catch (IOException e)
            {
                _log.info("Error on reading data, " + con.getClient().toString() + " disconnected abnormally!");
            }

            if (r <= 0)
            {
                closeClient(sk, con);
                return;
            }

            b.flip();
            boolean parsed = false;
            while (b.remaining() >= 2)
            {
                int size = b.getShort() & 0xFFFF;
                if (size <= b.remaining() + 2)
                {
                    // parse reading data
                    parsed = true;
                    parseData(con, size, b);
                }
                else
                    break;
            }

            // has no data remaining in buffer
            if (!b.hasRemaining())
            {
                releaseBuffer(b);
                con.readBuffer = null;
                return;
            }

            // has 1 byte remaining in buffer
            if (b.remaining() == 1)
            {
                // we don't know the packet size :(
                con.readBufferFirstByte = b.get();
                releaseBuffer(b);
                con.readBuffer = null;
                return;
            }

            if (parsed || b == sharedReadBuffer)
            {
                // allocate buffer for pending read
                con.readBuffer = null;
                int sz = b.getShort() & 0xFFFF;
                con.readBuffer = allocateBuffer(sz+2);
                con.readBuffer.putShort((short)sz).put(b);
                releaseBuffer(b);
            }
        }
        catch (Throwable t)
        {
            _log.log(Level.INFO, "", t);
            closeClient(sk, con);
        }
    }

    /** Parse received packet, push a message into inbound queue,
     * to be retrieved by scheduler.
     * 
     * @param con connection in which we received data
     * @param buf buffer with packet
     * @param sz size of the packet
     */
    private void parseData(MMOConnection con, int size, ByteBuffer buf) throws Throwable
    {
        try
        {
            int pos = buf.position();

            con.decrypt(buf, size - 2);
            buf.position(pos);

            if (buf.hasRemaining())
            {
                // apply limit
                int limit = buf.limit();
                buf.limit(pos + size - 2);
                con.addReceivedMsg(buf);
                buf.limit(limit);
            }

            buf.position(pos + size - 2);
        }
        catch (Throwable t)
        {
            _log.log(Level.SEVERE, "", t);
            releaseBuffer(buf);
            con.readBuffer = null;
        }
    }

    /** Pack (encode) a message into a network buffer.
     * The shared buffer is used.
     * 
     * @param msg message to pack
     * @return a buffer with data
     */
    private boolean pack(ServerBasePacket msg, SelectionKey sk, MMOConnection con)
    {
        if (Config.ASSERT) assert msg.getConnection().writeBuffer == null;
        sharedWriteBuffer.clear();

        try
        {
            boolean ok = msg.write(sharedWriteBuffer);
            if (!ok)
                return false;
        }
        catch (Exception e)
        {
            closeClient(sk, con);
            return false;
        }

        if (Config.ASSERT) assert sharedWriteBuffer.position() == 0;
        if (Config.ASSERT) assert sharedWriteBuffer.limit() >= 3;
        return true;
    }

    private void finishConnection(SelectionKey sk, MMOConnection con)
    {
        try
        {
            ((SocketChannel) sk.channel()).finishConnect();
        }
        catch (IOException e)
        {
            closeClient(sk, con);
        }

        // key might have been invalidated on finishConnect()
        if (sk.isValid())
        {
            sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
            sk.interestOps(sk.interestOps() & ~SelectionKey.OP_CONNECT);
        }
    }

    /** Accepts connection, creates a new channel, client, etc */
    private void acceptConnection(SelectionKey sk)
    {
        ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
        SocketChannel sc;

        try
        {
            if (sk.isAcceptable())
            {
                while ((sc = ssc.accept()) != null)
                {
                    if (_ipv4.accept(sc))
                    {
                        sc.configureBlocking(false);
                        sc.register(sk.selector(), SelectionKey.OP_READ|SelectionKey.OP_WRITE);
                        SelectionKey sk2 = sc.keyFor(sk.selector());
                        ClientThread client = new ClientThread(sc.socket());
                        sk2.attach(client.getConnection());
                    }
                    else
                        sc.socket().close();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /** Send a message to client.
     * The message is placed into message queue, and method returns immediately.
     * 
     * @param msg a message to send
     */
    public void sendMessage(ServerBasePacket msg)
    {
        // the message queue is synchronized itself
        if (Config.ASSERT) assert msg.getConnection() != null;

        if (msg.getLifeTime() > 0)
            msg.setTime(System.currentTimeMillis());

        sendMsgQueue.put(msg);
        msgCounter++;

        if (msgCounter > 20 || msg instanceof LeaveWorld)
        {
            _selector.wakeup();
            if (msgCounter > 500)
            {
                setPriority(Thread.NORM_PRIORITY+3); // up
            }
        }
    }

    /** Receives incoming messages.
     * Must be called by scheduler, which will read messages and dispatch
     * their execution to worker threads.
     * 
     * @return
     */
    private synchronized ClientBasePacket recvMessage()
    {
        return (ClientBasePacket)recvMsgQueue.get();
    }

    /** Scan outbound queue.
     * If a client's channel is free (the client has no
     * write buffer attached, with unfinished message), then
     * remove a message from queue, encode it into network
     * packet and send.
     * If message was not sent completely - store write
     * buffer in client's object, and set write interest for Selector.
     * 
     * @param selector
     */
    private void processOutboudQueue()
    {
        if (sendMsgQueue.isEmpty())
            return;

        Iterator<BasePacket> iter = sendMsgQueue.iterator();
        while (iter.hasNext())
        {
            ServerBasePacket msg = (ServerBasePacket)iter.next();
            if (msg.getLifeTime() > 0)
            {
                if (System.currentTimeMillis()-msg.getTime()>msg.getLifeTime())
                {
                    iter.remove();
                    continue;
                }
            }

            MMOConnection con = msg.getConnection();
            SelectionKey sk = con.getChannel().keyFor(_selector);
            if (sk == null || !sk.isValid())
                continue; // drop message

            iter.remove(); // remove the message
            if (Config.ASSERT) assert (sk.interestOps() & SelectionKey.OP_READ) != 0;
            if (con.writeBuffer != null)
            {
                if (Config.ASSERT) assert (sk.interestOps() & SelectionKey.OP_WRITE) != 0;
                // move the message into client's queue
                con._sendMsgQueue.put(msg);
                continue;
            }

            boolean ok = pack(msg, sk, con); // packs into shared writeBuffer
            if (!ok)
                continue;

            int r = 0;
            try
            {
                // try to write
                r = con.getChannel().write(sharedWriteBuffer);
            }
            catch (IOException e)
            {
                r = -1;
            }

            if (r < 0 || !sk.isValid())
            {
                closeClient(sk, con);
                return;
            }

            if (sharedWriteBuffer.hasRemaining())
            {
                // move remaining data in buffer to connection
                ByteBuffer b = allocateBuffer(sharedWriteBuffer.remaining());
                try
                {
                    b.put(sharedWriteBuffer);
                    b.flip();
                    con.writeBuffer = b;
                    con.writeTimeStamp = GameTimeController.getGameTicks();
                }
                catch (BufferOverflowException buffer)
                {
                    releaseBuffer(b);
                    buffer.printStackTrace();
                }

                if (sk.isValid())
                    sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE); 
                else
                    return;	// selection key invalid, what should we do ?  just return ?
            }
            else
            {
                // take first message from connection
                if (!con._sendMsgQueue.isEmpty())
                {
                    msg = (ServerBasePacket)con._sendMsgQueue.get();
                    if (msg != null)
                        sendMsgQueue.put(msg);
                }
            }
        }
    }

    /** A helper method to close client if connection was
     * closed or error occured. Frees all buffers, close
     * connections and so on.
     * 
     * @param c - a client object
     */
    private void closeClient(SelectionKey key, MMOConnection c)
    {
        // disconnect client
        c.onClientClose();

        if (c.readBuffer != null)
        {
            releaseBuffer(c.readBuffer);
            c.readBuffer = null;
        }

        if (c.writeBuffer != null)
        {
            releaseBuffer(c.writeBuffer);
            c.writeBuffer = null;
        }

        // clear attachment
        key.attach(null);
        // cancel key
        key.cancel();
    }

    /**
     * @return
     */
    public int inboundQueueSize()
    {
        return recvMsgQueue.size();
    }
    public int outboundQueueSize()
    {
        return sendMsgQueue.size();
    }
}