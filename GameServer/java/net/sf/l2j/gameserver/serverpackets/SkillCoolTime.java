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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TimeStamp;

/**
 * @author KenM
 */
public class SkillCoolTime extends L2GameServerPacket
{
	private final L2PcInstance _cha;
	
	public SkillCoolTime(L2PcInstance cha)
	{
		_cha = cha;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xc1);
		writeD(_cha.getReuseTimeStamps().size()); // list size
		for (TimeStamp ts : _cha.getReuseTimeStamps())
		{
			writeD(ts.getSkillId());
			
			writeD(_cha.getSkillLevel(ts.getSkillId()));
			writeD((int) ts.getReuse() / 1000);
			writeD((int) ts.getRemaining() / 1000);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] C1 SkillCoolTime";
	}
}