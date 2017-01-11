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
package net.sf.l2j.gameserver.pathfinding.cellnodes;

import net.sf.l2j.gameserver.pathfinding.AbstractNode;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;

public class CellNode extends AbstractNode
{
	private CellNode _next = null;
	private boolean _isInUse = true;
	private float _cost = -1000;

	public CellNode(AbstractNodeLoc loc)
	{
		super(loc);
	}

	public boolean isInUse()
	{
		return _isInUse;
	}

	public void setInUse()
	{
		_isInUse = true;
	}

	public CellNode getNext()
	{
		return _next;
	}

	public void setNext(CellNode next)
	{
		_next = next;
	}

	public float getCost()
	{
		return _cost;
	}

	public void setCost(double cost)
	{
		_cost = (float)cost;
	}

	public void free()
	{
		setParent(null);
		_cost = -1000;
		_isInUse = false;
		_next = null;
	}
}