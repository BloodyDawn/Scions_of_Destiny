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
package net.sf.l2j.gameserver.pathfinding.geonodes;

import net.sf.l2j.gameserver.pathfinding.AbstractNode;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;

/**
 *
 * @author -Nemesiss-
 */
public class GeoNode extends AbstractNode
{
	private final int _neighborsIdx;
	private short _cost;
	private GeoNode[] _neighbors;

	public GeoNode(AbstractNodeLoc Loc, int Neighbors_idx)
	{
		super(Loc);
		_neighborsIdx = Neighbors_idx;
	}

	public short getCost()
	{
		return _cost;
	}

	public void setCost(int cost)
	{
		_cost = (short)cost;
	}

	public GeoNode[] getNeighbors()
	{
		return _neighbors;
	}

	public void attachNeighbors()
	{
		if(getLoc() == null) _neighbors = null;
		else _neighbors = GeoPathFinding.getInstance().readNeighbors(this, _neighborsIdx);
	}
}