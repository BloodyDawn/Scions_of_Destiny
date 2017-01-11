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
package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class VoiceExperience implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS =
    {
        "expon",
        "expoff"
    };
   
    @Override
    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
    {
        if (command.equalsIgnoreCase("expon"))
        {
            activeChar.setGainXpSp(true);
            activeChar.sendMessage("Experience Gain: Enabled.");
            activeChar.sendMessage("Skill Point Gain: Enabled.");
        }
        else if (command.equalsIgnoreCase("expoff"))
        {
            activeChar.setGainXpSp(false);
            activeChar.sendMessage("Experience Gain: Disabled.");
            activeChar.sendMessage("Skill Point Gain: Disabled.");
        }
        return true;
    }

    @Override
    public String[] getVoicedCommandList()
    {
        return VOICED_COMMANDS;
    }
}