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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2BabyPetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Beast SpiritShot Handler
 * @author Tempy
 */
public class BeastSpiritShot implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static int[] _itemIds =
	{
		6646,
		6647
	};

	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (playable == null)
		{
			return;
		}

		L2PcInstance activeOwner = null;

		if (playable instanceof L2PcInstance)
		{
			activeOwner = (L2PcInstance) playable;
		}
		else if (playable instanceof L2Summon)
		{
			activeOwner = ((L2Summon) playable).getOwner();
			activeOwner.sendPacket(new SystemMessage(SystemMessage.PET_CANNOT_USE_ITEM));
			return;
		}

		if (activeOwner == null)
		{
			return;
		}

		L2Summon activePet = activeOwner.getPet();

		if (activePet == null)
		{
			activeOwner.sendPacket(new SystemMessage(574));
			return;
		}

		if (activePet.isDead())
		{
			activeOwner.sendPacket(new SystemMessage(1598));
			return;
		}

		int itemId = item.getItemId();
		boolean isBlessed = (itemId == 6647);

		// Blessed Beast Spirit Shot cannot be used in olympiad.
		if (isBlessed && activeOwner.isInOlympiadMode())
		{
			activeOwner.sendPacket(new SystemMessage(SystemMessage.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		short shotConsumption = activePet.getSpiritShotsPerHit();
		int shotCount = item.getCount();

		if (shotCount < shotConsumption)
		{
			// Not enough SpiritShots to use.
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(new SystemMessage(1700));
			}
			return;
		}

		L2ItemInstance weaponInst = null;

		if ((activePet instanceof L2PetInstance) && !(activePet instanceof L2BabyPetInstance))
		{
			weaponInst = ((L2PetInstance) activePet).getActiveWeaponInstance();
		}

		if (weaponInst == null)
		{

			if (activePet.getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
			{
				return;
			}

			if (isBlessed)
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}
		else
		{
			if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			{
				// SpiritShots are already active.
				return;
			}

			if (isBlessed)
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}

		if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
		{
			if (!activeOwner.disableAutoShot(itemId))
			{
				activeOwner.sendPacket(new SystemMessage(1700));
			}
			return;
		}

		// Pet uses the power of spirit.
		activeOwner.sendPacket(new SystemMessage(1576));

		Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUse(activePet, activePet, isBlessed ? 2009 : 2008, 1, 0, 0), 360000/* 600 */);
	}

	@Override
	public int[] getItemIds()
	{
		return _itemIds;
	}
}