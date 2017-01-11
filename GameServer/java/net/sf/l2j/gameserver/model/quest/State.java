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
package net.sf.l2j.gameserver.model.quest;

/**
 * @author Luis Arias
 *
 * Functions in this class are used in python files 
 */
public class State
{
    /** Prototype of empty String list */
    private static final String[] emptyStrList = new String[0];

    /** Quest object associated to the state */
    private final Quest _quest;

    private String[] _Events = emptyStrList;

    /** Name of the quest */
    private String _Name;

    /**
     * Constructor for the state of the quest. 
     * @param name : String pointing out the name of the quest
     * @param quest : Quest
     */
    public State(String name, Quest quest)
    {
        _Name = name;
	_quest = quest;
	quest.addState(this);
    }

    /**
     * Return list of events
     * @return String[]
     */
    public String[] getEvents()
    {
        return _Events;
    }

    /**
     * Return name of the quest
     * @return String
     */
    public String getName()
    {
        return _Name;
    }

    /**
     * Return name of the quest
     * @return String
     */
    public String toString()
    {
        return _Name;
    }

    public Quest getQuest()
    {
        return _quest;
    }
}