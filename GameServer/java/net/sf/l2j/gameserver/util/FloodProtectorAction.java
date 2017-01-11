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
package net.sf.l2j.gameserver.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.StringUtil;

/**
 * Flood protector implementation.
 * 
 * @author fordfrog
 */
public final class FloodProtectorAction
{
    /**
     * Logger
     */
    private static final Logger _log = Logger.getLogger(FloodProtectorAction.class.getName());

    /**
     * Client for this instance of flood protector.
     */
    private final ClientThread _client;

    /**
     * Configuration of this instance of flood protector.
     */
    private final FloodProtectorConfig _config;

    /**
     * Next game tick when new request is allowed.
     */
    private volatile int _nextGameTick = GameTimeController.getGameTicks();

    /**
     * Request counter.
     */
    private AtomicInteger _count = new AtomicInteger(0);

    /**
     * Flag determining whether exceeding request has been logged.
     */
    private boolean _logged;

    /**
     * Flag determining whether punishment application is in progress so that we do not apply
     * punisment multiple times (flooding).
     */
    private volatile boolean _punishmentInProgress;
	
    /**
     * Creates new instance of FloodProtectorAction.
     * 
     * @param player
     *            player for which flood protection is being created
     * @param config
     *            flood protector configuration
     */
    public FloodProtectorAction(final ClientThread client, final FloodProtectorConfig config)
    {
        super();
        _client = client;
        _config = config;
    }

    /**
     * Checks whether the request is flood protected or not.
     * 
     * @param command
     *            command issued or short command description
     * 
     * @return true if action is allowed, otherwise false
     */
    public boolean tryPerformAction(final String command)
    {
        final int curTick = GameTimeController.getGameTicks();

        if (curTick < _nextGameTick || _punishmentInProgress)
        {
            if (_config.LOG_FLOODING && !_logged && _log.isLoggable(Level.WARNING))
            {
                log("called command ", command, " ~", String.valueOf((_config.FLOOD_PROTECTION_INTERVAL - (_nextGameTick - curTick)) * GameTimeController.MILLIS_IN_TICK), " ms after previous command");
                _logged = true;
            }

            _count.incrementAndGet();

            if (!_punishmentInProgress && _config.PUNISHMENT_LIMIT > 0 && _count.get() >= _config.PUNISHMENT_LIMIT && _config.PUNISHMENT_TYPE != null)
            {
                _punishmentInProgress = true;

                if ("kick".equals(_config.PUNISHMENT_TYPE))
                    kickPlayer();
                else if ("ban".equals(_config.PUNISHMENT_TYPE))
                    banAccount();
                else if ("jail".equals(_config.PUNISHMENT_TYPE))
                    jailChar();

                _punishmentInProgress = false;
            }

            return false;
        }

        if (_count.get() > 0)
        {
            if (_config.LOG_FLOODING && _log.isLoggable(Level.WARNING))
                log("issued ", String.valueOf(_count), " extra requests within ~", String.valueOf(_config.FLOOD_PROTECTION_INTERVAL * GameTimeController.MILLIS_IN_TICK), " ms");
        }

        _nextGameTick = curTick + _config.FLOOD_PROTECTION_INTERVAL;
        _logged = false;
        _count.set(0);

        return true;
    }

    /**
     * Kick player from game (close network connection).
     */
    private void kickPlayer()
    {
        if (_log.isLoggable(Level.WARNING))
            log("was kicked for flooding");

        if (_client.getActiveChar() != null)
            _client.getActiveChar().closeNetConnection();
        else
            _client.getConnection().close(true);
    }

    /**
     * Bans account and char if possible and logs out the char.
     */
    private void banAccount()
    {
        if (_log.isLoggable(Level.WARNING))
            log("was banned for flooding");

        if (_client.getLoginName() != null)
            LoginServerThread.getInstance().sendAccessLevel(_client.getLoginName(), -100);

        if (_client.getActiveChar() != null)
        {
            _client.getActiveChar().setAccessLevel(-100);
            _client.getActiveChar().closeNetConnection();
        }
        else
            _client.getConnection().close(true);
    }

    /**
     * Jails char.
     */
    private void jailChar()
    {
        if (_client.getActiveChar() != null)
        {
            if (_log.isLoggable(Level.WARNING))
                log("was jailed for flooding ", _config.PUNISHMENT_TIME <= 0 ? "forever" : "for " + _config.PUNISHMENT_TIME + " mins");

            if (!_client.getActiveChar().isInJail())
                _client.getActiveChar().setInJail(true, _config.PUNISHMENT_TIME);
            else
                _client.getActiveChar().closeNetConnection();
        }
        else
            log("unable to jail: no active player");
    }

    private void log(String... lines)
    {
        final StringBuilder output = StringUtil.startAppend(100, _config.FLOOD_PROTECTOR_TYPE, ": ");

        switch (_client.getState())
        {
            case IN_GAME:
                if (_client.getActiveChar() != null)
                {
                    StringUtil.append(output, _client.getActiveChar().getName());
                    StringUtil.append(output, "(", String.valueOf(_client.getActiveChar().getObjectId()),") ");
                }
                break;
            case AUTHED:
                if (_client.getLoginName() != null)
                    StringUtil.append(output, _client.getLoginName()," ");
                break;
            case CONNECTED:
                String address = _client.getConnection().getIP();
                if (address != null)
                    StringUtil.append(output, address);
                break;
            default:
                throw new IllegalStateException("Missing state on switch");
        }

        StringUtil.append(output, lines);
        _log.warning(output.toString());
    }
}