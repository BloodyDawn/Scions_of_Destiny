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
package net.sf.l2j.gameserver.model.entity;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Jamaica
 *
 */
public class AutoRewarder
{
    private static List<String> _ips;

    public static void load()
    {
        _ips = new ArrayList<>();

        System.out.println("Initializing Auto Rewarder");
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                autoReward();
            }
	
        }, Config.AUTO_REWARD_DELAY * 1000, Config.AUTO_REWARD_DELAY * 1000);
    }

    public static void autoReward()
    {
        for (L2PcInstance p : L2World.getInstance().getAllPlayers())
        {
            if (p == null)
                continue;

            if (p.inOfflineMode() || p.isInJail())
                continue;

            if (!p.isConnected())
                continue;

            if (p.getNetConnection() == null)
                continue;

            String ip = p.getNetConnection().getIP();
            if (ip != null && _ips.contains(ip))
                continue;

            _ips.add(ip);

            p.addItem("autoReward", Config.AUTO_REWARD_ID, Config.AUTO_REWARD_COUNT, p, true);
        }
        _ips.clear();
    }
}