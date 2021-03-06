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

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;

/**
 * This class ...
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PartySpelled extends L2GameServerPacket
{
	private static final String _S__EE_PartySpelled = "[S] EE PartySpelled";
	private final List<Effect> _effects;
	private final L2Character _char;
	
	class Effect
	{
		int skillId;
		int dat;
		int duration;
		
		public Effect(int pSkillId, int pDat, int pDuration)
		{
			skillId = pSkillId;
			dat = pDat;
			duration = pDuration;
		}
	}
	
	public PartySpelled(L2Character cha)
	{
		_effects = new FastList<>();
		_char = cha;
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_char == null)
		{
			return;
		}
		
		writeC(0xee);
		writeD(_char instanceof L2SummonInstance ? 2 : _char instanceof L2PetInstance ? 1 : 0);
		writeD(_char.getObjectId());
		
		// C4 doesn't support more than 20 effects
		// in party window, so limiting them makes no difference.
		// This check ignores first effects, so there is space
		// for last effects to be viewable by party members.
		// It may also help healers be aware of cursed members.
		int size = 0;
		if (_effects.size() > 20)
		{
			writeD(20);
			size = _effects.size() - 20;
		}
		else
		{
			writeD(_effects.size());
		}
		
		for (; size < _effects.size(); size++)
		{
			Effect temp = _effects.get(size);
			if (temp == null)
			{
				continue;
			}
			
			writeD(temp.skillId);
			writeH(temp.dat);
			writeD(temp.duration / 1000);
		}
	}
	
	public void addPartySpelledEffect(int skillId, int dat, int duration)
	{
		_effects.add(new Effect(skillId, dat, duration));
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__EE_PartySpelled;
	}
}