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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.MonsterRace;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.serverpackets.MonRaceInfo;
import net.sf.l2j.gameserver.serverpackets.PlaySound;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands: - invul = turns invulnerability
 * on/off
 * 
 * @version $Revision: 1.1.6.4 $ $Date: 2005/04/11 10:06:00 $
 */
public class AdminMonsterRace implements IAdminCommandHandler
{
    //private static Logger _log = Logger.getLogger(AdminMonsterRace.class.getName());

    private static String[] _adminCommands = {"admin_mons"};

    private static final int REQUIRED_LEVEL = Config.GM_MONSTERRACE;
    protected static int state = -1;

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        if (!Config.ALT_PRIVILEGES_ADMIN)
        {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
            {
                return false;
            }
        }

        if (command.equalsIgnoreCase("admin_mons"))
        {
            handleSendPacket(activeChar);
        }
        return true;
    }

    public String[] getAdminCommandList()
    {
        return _adminCommands;
    }

    private boolean checkLevel(int level)
    {
        return (level >= REQUIRED_LEVEL);
    }

    private void handleSendPacket(L2PcInstance activeChar)
    {
        /*
         * -1 0 to initial the race
         * 0 15322 to start race
         * 13765 -1 in middle of race
         * -1 0 to end the race
         * 
         * 8003 to 8027
         */

        int[][] codes = { {-1, 0}, {0, 15322}, {13765, -1}, {-1, 0}};
        MonsterRace race = MonsterRace.getInstance();

        if (state == -1)
        {
            state++;
            race.newRace();
            race.newSpeeds();
            MonRaceInfo spk = new MonRaceInfo(codes[state][0], codes[state][1], race.getMonsters(),
                                              race.getSpeeds());
            activeChar.sendPacket(spk);
            activeChar.broadcastPacket(spk);
        }
        else if (state == 0)
        {
            state++;
            SystemMessage sm = new SystemMessage(824);
            sm.addNumber(0);
            activeChar.sendPacket(sm);
            PlaySound SRace = new PlaySound(1, "S_Race", 0, 0, 0, 0, 0);
            activeChar.sendPacket(SRace);
            activeChar.broadcastPacket(SRace);
            PlaySound SRace2 = new PlaySound(0, "ItemSound2.race_start", 1, 121209259, 12125, 182487,
                                             -3559);
            activeChar.sendPacket(SRace2);
            activeChar.broadcastPacket(SRace2);
            MonRaceInfo spk = new MonRaceInfo(codes[state][0], codes[state][1], race.getMonsters(),
                                              race.getSpeeds());
            activeChar.sendPacket(spk);
            activeChar.broadcastPacket(spk);

            ThreadPoolManager.getInstance().scheduleGeneral(new RunRace(codes, activeChar), 5000);
        }

    }

    class RunRace implements Runnable
    {

        private int[][] codes;
        private L2PcInstance activeChar;

        public RunRace(int[][] pCodes, L2PcInstance pActiveChar)
        {
            this.codes = pCodes;
            this.activeChar = pActiveChar;
        }

        public void run()
        {
            MonRaceInfo spk = new MonRaceInfo(codes[2][0], codes[2][1], MonsterRace.getInstance().getMonsters(), MonsterRace.getInstance().getSpeeds());
            activeChar.sendPacket(spk);
            activeChar.broadcastPacket(spk);
            ThreadPoolManager.getInstance().scheduleGeneral(new RunEnd(activeChar), 30000);
        }
    }

    class RunEnd implements Runnable
    {
        private L2PcInstance activeChar;

        public RunEnd(L2PcInstance pActiveChar)
        {
            this.activeChar = pActiveChar;
        }

        public void run()
        {
            DeleteObject obj = null;
            for (int i = 0; i < 8; i++)
            {
                obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
                activeChar.sendPacket(obj);
                activeChar.broadcastPacket(obj);
            }
            state = -1;
        }
    }
}