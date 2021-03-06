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

package net.sf.l2j.gameserver.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/* 
 * coded by Balancer
 * balancer@balancer.ru
 * http://balancer.ru
 *
 * version 0.1, 2005-06-06
 */
public class Log
{
    private static final Logger _log = Logger.getLogger(Log.class.getName());

    public static final void add(String text, String cat)
    {
        String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());

        new File("log/game").mkdirs();

        File file = new File("log/game/"+(cat!=null?cat:"_all")+".txt");
        try (FileWriter save = new FileWriter(file, true))
        {
            String out = "["+date+"] '---': "+text+"\n";
            save.write(out);
            save.flush();
            file = null;
        }
        catch (IOException e)
        {
            _log.warning("saving chat log failed: " + e);
            e.printStackTrace();
        }

        if (cat != null)
            add(text, null);
    }

    public static final void addEvent(L2PcInstance pc, String text)
    {
        String date = (new SimpleDateFormat("yy.MM.dd H:mm:ss")).format(new Date());
        String filedate = (new SimpleDateFormat("yyMMdd_H")).format(new Date());

        new File("log/game").mkdirs();

        File file = new File("log/game/actions_"+filedate+".txt");
        try (FileWriter save = new FileWriter(file, true))
        {
            
            String out = "["+date+"] '<"+pc.getName()+">': "+text+"\n"; // "+char_name()+"
            save.write(out);
        }
        catch (IOException e)
        {
            _log.warning("saving actions log failed: " + e);
            e.printStackTrace();
        }
    }

    public static final void Assert(boolean exp)
    {
        Assert(exp,"");
    }

    public static final void Assert(boolean exp, String cmt)
    {
        if (exp || !Config.ASSERT)
            return;

        System.out.println("Assertion error ["+cmt+"]");
        Thread.dumpStack();
    }
}