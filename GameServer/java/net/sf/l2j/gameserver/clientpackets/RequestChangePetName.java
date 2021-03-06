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
package net.sf.l2j.gameserver.clientpackets;

import net.sf.l2j.gameserver.datatables.PetNameTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.3.4.4 $ $Date: 2005/04/06 16:13:48 $
 */
public class RequestChangePetName extends L2GameClientPacket
{
	private static final String REQUESTCHANGEPETNAME__C__89 = "[C] 89 RequestChangePetName";
	// private static Logger _log = Logger.getLogger(RequestChangePetName.class.getName());

	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		final L2Summon pet = activeChar.getPet();
		if (pet == null)
		{
			return;
		}

		if (pet.getName() != null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET));
			return;
		}

		if (PetNameTable.getInstance().doesPetNameExist(_name, pet.getTemplate().npcId))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NAMING_ALREADY_IN_USE_BY_ANOTHER_PET));
			return;
		}

		if ((_name.length() < 1) || (_name.length() > 16))
		{
			
			activeChar.sendMessage("Your pet's name can be up to 16 characters.");
			return;
		}

		if (!PetNameTable.getInstance().isValidPetName(_name))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NAMING_PETNAME_CONTAINS_INVALID_CHARS));
			return;
		}

		pet.setName(_name);

		pet.updateAndBroadcastStatus(1);
	}

	@Override
	public String getType()
	{
		return REQUESTCHANGEPETNAME__C__89;
	}
}