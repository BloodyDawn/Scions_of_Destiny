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

import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread.SessionKey;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.CharSelectInfoPackage;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.util.EventData;

/**
 * This class ...
 * 
 * @version $Revision: 1.21.2.19.2.12 $ $Date: 2005/04/04 19:47:01 $
 */
@SuppressWarnings("rawtypes")
public final class ClientThread
{
    protected static final Logger _log = Logger.getLogger(ClientThread.class.getName());

    /**
     * CONNECTED	- client has just connected
     * AUTHED		- client has authed but doesnt has character attached to it yet
     * IN_GAME		- client has selected a char and is in game
     * @author  KenM
     */
    public static enum GameClientState
    {
        CONNECTED,
        AUTHED,
        IN_GAME
    };

    public GameClientState state;

    private boolean _protocol;

    private String _loginName;
    private L2PcInstance _activeChar;
    private SessionKey _sessionId;
    private ReentrantLock _activeCharLock = new ReentrantLock();
    private final MMOConnection _connection;

    final ScheduledFuture _autoSaveInDB;

    private int _revision = 0;
    private boolean _gameGuardOk = false;

    // Flood protection
    public byte packetsSentInSec = 0;
    public int packetsSentStartTick = 0;

    // floodprotectors
    private final FloodProtectors _floodProtectors = new FloodProtectors(this);

    private List<Integer> _charSlotMapping = new FastList<>();

    public ClientThread(Socket socket)
    {
        state = GameClientState.CONNECTED;
        _connection = new MMOConnection(this, socket);
        _autoSaveInDB = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoSaveTask(), 300000L, 900000L);
    }

    public void onDisconnect()
    {
        try
        {
            _autoSaveInDB.cancel(false);
            L2PcInstance player = getActiveChar();
            if (player != null)  // this should only happen on connection loss
            {
                // For better synchronization do it now
                setActiveChar(null);

                if (player.isLocked())
                    _log.log(Level.WARNING, "Player "+player.getName()+" still performing subclass actions during disconnect.");

                // we store all data from players who are disconnected while in an event in order to restore it in the next login
                if (player.atEvent)
                {
                    EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventkarma, player.eventpvpkills, player.eventpkkills, player.eventTitle, player.kills, player.eventSitForced);
                    L2Event.connectionLossData.put(player.getName(), data);
                }

                if (player.isFlying())
                    player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));

                // notify the world about our disconnect
                player.deleteMe();

                try
                {
                    saveCharToDisk(player);
                }
                catch (Exception e2)
                {
                    /* ignore any problems here */
                }
            }
        }
        catch (Exception e1)
        {
            _log.log(Level.WARNING, "error while disconnecting client", e1);
        }
    }

    /**
     * Produces the best possible string representation of this client.
     */
    public String toString()
    {
        try
        {
            switch (getState())
            {
                case CONNECTED:
                    return "[IP: "+(_connection.getIP() == null ? "'?'" : _connection.getIP())+"]";
                case AUTHED:
                    return "[Account: "+getLoginName()+" - IP: "+(_connection.getIP() == null ? "'?'" : _connection.getIP())+"]";
                case IN_GAME:
                    return "[Character: "+(getActiveChar() == null ? "'?'" : getActiveChar().getName())+" - Account: "+getLoginName()+" - IP: "+(_connection.getIP() == null ? "'?'" : _connection.getIP())+"]";
                default:
                    throw new IllegalStateException("Missing state on switch");
            }
        }
        catch (NullPointerException e)
        {
            return "[Character read failed due to disconnect]";
        }
    }

    /**
     * Save the L2PcInstance to the database.
     */
    public static void saveCharToDisk(L2PcInstance cha)
    {
        try
        {
            cha.store();
            if (Config.UPDATE_ITEMS_ON_CHAR_STORE)
                cha.getInventory().updateDatabase();
        }
        catch(Exception e)
        {
            _log.warning("Error saving player character: "+e);
        }
    }

    public void markRestoredChar(int charslot) throws Exception
    {	
        //have to make sure active character must be nulled
        int objid = getObjectIdForSlot(charslot);
        if (objid < 0)
            return;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_Id=?"))
        {
            statement.setInt(1, objid);
            statement.execute();
        }
        catch (Exception e)
        {
            _log.warning("Data error on restoring char: " + e);
        }
    }

    /**
     * Method to handle character deletion
     *
     * @return a byte:
     * <li>-1: Error: No char was found for such charslot, caught exception, etc...
     * <li> 0: character is not member of any clan, proceed with deletion
     * <li> 1: character is member of a clan, but not clan leader
     * <li> 2: character is clan leader
     */
    public byte markToDeleteChar(int charslot)
    {
        int objid = getObjectIdForSlot(charslot);
        if (objid < 0)
            return -1;

        byte answer = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection())
        {
            int clanId = 0;

            try (PreparedStatement statement = con.prepareStatement("SELECT clanId from characters WHERE obj_Id=?"))
            {
                statement.setInt(1, objid);
                try (ResultSet rs = statement.executeQuery())
                {
                    rs.next();
                    clanId = rs.getInt(1);
                }
            }

            if (clanId != 0)
            {
                L2Clan clan = ClanTable.getInstance().getClan(clanId);
                if (clan != null)
                {
                    if (clan.getLeaderId() == objid)
                        answer = 2;
                    else
                        answer = 1;
                }
            }

            // Setting delete time
            if (answer == 0)
            {
                if (Config.DELETE_DAYS == 0)
                    deleteCharByObjId(objid);
                else
                {
                    try (PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_Id=?"))
                    {
                        statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS*86400000); // 24*60*60*1000 = 86400000
                        statement.setInt(2, objid);
                        statement.execute();
                    }
                }
            }
        }
        catch (Exception e)
        {
            _log.warning("Data error on update delete time of char: " + e);
            return -1;
        }

        return answer;
    }

    public static void deleteCharByObjId(int objid)
    {
	if (objid < 0)
	    return;

	try (Connection con = L2DatabaseFactory.getInstance().getConnection())
        {
            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? OR friend_id=?"))
            {
                statement.setInt(1, objid);
                statement.setInt(2, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM heroes WHERE char_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE char_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM character_recommends WHERE char_id=? OR target_id=?"))
            {
                statement.setInt(1, objid);
                statement.setInt(2, objid);
                statement.execute();
            }

            try (PreparedStatement statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?"))
            {
                statement.setInt(1, objid);
                statement.execute();
            }
        }
        catch (Exception e)
        {
            _log.warning("Data error on deleting char: " + e);
        }
    }

    public L2PcInstance loadCharFromDisk(int charslot)
    {
        L2PcInstance character = L2PcInstance.load(getObjectIdForSlot(charslot));
        if (character != null)
        {
            // preinit some values for each login
            character.setRunning(); // running is default
            character.standUp();    // standing is default

            character.refreshOverloaded();
            character.refreshExpertisePenalty();
            character.setOnlineStatus(true);
        }
        else
            _log.warning("could not restore in slot:"+ charslot);
		
        return character;
    }

    /**
     * @param charslot
     * @return
     */
    private int getObjectIdForSlot(int charslot)
    {
        if (charslot < 0 || charslot >= _charSlotMapping.size())
        {
            _log.warning(toString() + " tried to delete Character in slot "+charslot+" but no characters exits at that slot.");
            return -1;
        }
        Integer objectId = _charSlotMapping.get(charslot);
        return objectId.intValue();
    }

    /**
     * @return
     */
    public MMOConnection getConnection()
    {
        return _connection;
    }

    /**
     * @return
     */
    public L2PcInstance getActiveChar()
    {
        return _activeChar;
    }

    /**
     * @return Returns the sessionId.
     */
    public SessionKey getSessionId()
    {
        return _sessionId;
    }

    public String getLoginName()
    {
        return _loginName;
    }

    public void setLoginName(String loginName)
    {
        _loginName = loginName;
    }

    /**
     * @param cha
     */
    public void setActiveChar(L2PcInstance cha)
    {
        _activeChar = cha;
        if (cha != null)
            L2World.getInstance().storeObject(getActiveChar());
    }

    public ReentrantLock getActiveCharLock()
    {
        return _activeCharLock;
    }

    public FloodProtectors getFloodProtectors()
    {
        return _floodProtectors;
    }

    /**
     * @param key
     */
    public void setSessionId(SessionKey key)
    {
        _sessionId = key;
    }

    /**
     * @param chars
     */
    public void setCharSelection(CharSelectInfoPackage[] chars)
    {
        _charSlotMapping.clear();

        for (int i = 0; i < chars.length; i++)
        {
            int objectId = chars[i].getObjectId();
            _charSlotMapping.add(new Integer(objectId));
        }
    }

    /**
     * @return Returns the revision.
     */
    public int getRevision()
    {
        return _revision;
    }

    /**
     * @param revision The revision to set.
     */
    public void setRevision(int revision)
    {
        _revision = revision;
    }

    public void setGameGuardOk(boolean gameGuardOk)
    {
        _gameGuardOk = gameGuardOk;
    }

    public boolean isGameGuardOk()
    {
        return _gameGuardOk;
    }

    public boolean isProtocolOk()
    {
        return _protocol;
    }

    public void setProtocolOk(boolean b)
    {
        _protocol = b;
    }

    public GameClientState getState()
    {
        return state;
    }

    public void setState(GameClientState pState)
    {
        state = pState;
    }

    class AutoSaveTask implements Runnable
    {
        public void run()
        {
            try
            {
                L2PcInstance player = getActiveChar();
                if (player != null)
                    saveCharToDisk(player);
            }
            catch (Exception e)
            {
                _log.severe(e.toString());
            }
        }
    }
}