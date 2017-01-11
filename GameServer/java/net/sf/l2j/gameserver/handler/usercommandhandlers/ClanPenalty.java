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
package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;

import javolution.text.TextBuilder;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;

/**
 * Support for clan penalty user command.  
 * @author Tempy
 */
public class ClanPenalty implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS = { 100 }; 

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (id != COMMAND_IDS[0])
            return false;

        boolean penalty = false;
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        TextBuilder htmlContent = new TextBuilder("<html><body>");
        htmlContent.append("<center><table width=270 border=0 bgcolor=111111>");
        htmlContent.append("<tr><td width=170>Penalty</td>");
        htmlContent.append("<td width=100 align=center>Expiration Date</td></tr>");
        htmlContent.append("</table><table width=270 border=0><tr>");

        if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
        {
            htmlContent.append("<tr><td width=170>Unable to join a clan.</td>");
            htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClanJoinExpiryTime())+"</td></tr>");
            penalty = true;
        }

        if (activeChar.getClanCreateExpiryTime() > System.currentTimeMillis())
        {
            htmlContent.append("<tr><td width=170>Unable to create a clan.</td>");
            htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClanCreateExpiryTime())+"</td></tr>");
            penalty = true;
        }

        if (activeChar.getClan() != null)
        {
            if (activeChar.getClan().getCharPenaltyExpiryTime() > System.currentTimeMillis())
            {
                htmlContent.append("<tr><td width=170>Unable to invite players to clan.</td>");
                htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClan().getCharPenaltyExpiryTime())+"</td></tr>");
                penalty = true;
            }

            if (activeChar.getClan().getRecoverPenaltyExpiryTime() > System.currentTimeMillis())
            {
                htmlContent.append("<tr><td width=170>Unable to dissolve clan.</td>");
                htmlContent.append("<td width=100 align=center>"+format.format(activeChar.getClan().getRecoverPenaltyExpiryTime())+"</td></tr>");
                penalty = true;
            }
        }

        if (!penalty)
        {
            htmlContent.append("<td width=170>No penalties currently in effect.</td>");
            htmlContent.append("<td width=100 align=center> </td>");
        }
		
        htmlContent.append("</tr></table><img src=\"L2UI.SquareWhite\" width=270 height=1>");
        htmlContent.append("</center></body></html>");
        
        NpcHtmlMessage penaltyHtml = new NpcHtmlMessage(0);
        penaltyHtml.setHtml(htmlContent.toString());
        activeChar.sendPacket(penaltyHtml);
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}