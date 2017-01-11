package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;


/**
 * This class handles following admin commands:
 * - server_shutdown [sec] = shows menu or shuts down server in sec seconds
 * 
 * @version $Revision: 1.6.3 $ $Date: 07/01/2017 16:00:23 $
 */
public class AdminShutdown implements IAdminCommandHandler
{
	//private static Logger _log = Logger.getLogger(AdminShutdown.class.getName());
	
	private static String[] _adminCommands = {"admin_server_shutdown", "admin_server_restart", "admin_server_abort"};
	private static final int REQUIRED_LEVEL = Config.GM_RESTART;
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
        {
                if (!Config.ALT_PRIVILEGES_ADMIN)
                {
                        if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
                                return false;
                }
		
		if (command.startsWith("admin_server_shutdown"))
		{	
			try
			{
				int val = Integer.parseInt(command.substring(22)); 
				serverShutdown(activeChar, val, false);
			}
			catch (StringIndexOutOfBoundsException e)
			{				
				sendHtmlForm(activeChar);
			}
		} else if (command.startsWith("admin_server_restart"))
		{	
			try
			{
				int val = Integer.parseInt(command.substring(21)); 
				serverShutdown(activeChar, val, true);
			}
			catch (StringIndexOutOfBoundsException e)
			{				
				sendHtmlForm(activeChar);
			}
		} else if (command.startsWith("admin_server_abort"))
		{	
			serverAbort(activeChar);
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

	private void sendHtmlForm(L2PcInstance activeChar)
        {
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		int t = GameTimeController.getInstance().getGameTime();
		int h = t/60;
		int m = t%60;
		SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Главная\" action=\"bypass -h admin_admin\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Меню управления сервером</center></td>");
		replyMSG.append("<td width=40><button value=\"Назад\" action=\"bypass -h admin_admin\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<table>");	
		replyMSG.append("<tr><td>Игроки онлайн: " + L2World.getInstance().getAllPlayersCount() + "</td></tr>");				
		replyMSG.append("<tr><td>Занятый объем памяти: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + " bytes</td></tr>");
		replyMSG.append("<tr><td>Рейты сервера: " + Config.RATE_XP + "x, " + Config.RATE_SP + "x, " + Config.RATE_DROP_ADENA + "x, " + Config.RATE_DROP_ITEMS + "x, " + Config.RATE_BOSS_DROP_ITEMS + "x</td></tr>");
		replyMSG.append("<tr><td>Время игры: " + format.format(cal.getTime()) + "</td></tr>");
		replyMSG.append("</table><br>");		
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td><center>Введите время в секундах до отключения сервера: <edit var=\"shutdown_time\" width=60></center></td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Выключение\" action=\"bypass -h admin_server_shutdown $shutdown_time\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");			
		replyMSG.append("<button value=\"Рестарт\" action=\"bypass -h admin_server_restart $shutdown_time\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");			
		replyMSG.append("<button value=\"Отмена\" action=\"bypass -h admin_server_abort\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><br>");
		replyMSG.append("</td></tr></table></center>");
        replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td><center>QuickBox: <edit var=\"menu_command\" width=120></center></td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Reload\" action=\"bypass -h admin_reload $menu_command\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Reload HTM\" action=\"bypass -h admin_cache_htm_rebuild\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Reload File\" action=\"bypass -h admin_cache_reload_file $menu_command\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
        replyMSG.append("<button value=\"Skill Test\" action=\"bypass -h admin_skill_test $menu_command\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br>");
		replyMSG.append("</td></tr></table></center>");	
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Reload CW\" action=\"bypass -h admin_cw_reload\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Reload Zone\" action=\"bypass -h admin_zone_reload\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Reload Crest\" action=\"bypass -h admin_cache_crest_rebuild\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
        replyMSG.append("<button value=\"Crest Fix\" action=\"bypass -h admin_cache_crest_fix\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br>");
		replyMSG.append("</td></tr></table></center>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Reload Tele\" action=\"bypass -h admin_teleport_reload\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Open All\" action=\"bypass -h admin_openall\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Close All\" action=\"bypass -h admin_closeall\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
        replyMSG.append("<button value=\"Zone Check\" action=\"bypass -h admin_zone_check\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br>");
		replyMSG.append("</td></tr></table></center>");
		replyMSG.append("<tr><td><center>Created by ZOOmby</center></td></tr>");
		replyMSG.append("</body></html>");
			
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);				
	}
	
	private void serverShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		Shutdown.getInstance().startShutdown(activeChar, seconds, restart);
	}
	
	private void serverAbort(L2PcInstance activeChar)
	{
		Shutdown.getInstance().abort(activeChar);
	}
}