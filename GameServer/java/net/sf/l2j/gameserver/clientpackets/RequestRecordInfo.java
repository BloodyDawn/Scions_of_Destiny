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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.serverpackets.CharInfo;
import net.sf.l2j.gameserver.serverpackets.DoorInfo;
import net.sf.l2j.gameserver.serverpackets.DoorStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.serverpackets.PetItemList;
import net.sf.l2j.gameserver.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.serverpackets.SpawnItem;
import net.sf.l2j.gameserver.serverpackets.SpawnItemPoly;
import net.sf.l2j.gameserver.serverpackets.StaticObject;
import net.sf.l2j.gameserver.serverpackets.UserInfo;

public class RequestRecordInfo extends L2GameClientPacket
{
	private static final String _0__CF_REQUEST_RECORD_INFO = "[0] CF RequestRecordInfo";
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		activeChar.sendPacket(new UserInfo(activeChar));
		
		for (L2Object object : activeChar.getKnownList().getKnownObjects().values())
		{
			if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
			{
				activeChar.sendPacket(new SpawnItemPoly(object));
			}
			else
			{
				if (object instanceof L2ItemInstance)
				{
					activeChar.sendPacket(new SpawnItem((L2ItemInstance) object));
				}
				else if (object instanceof L2DoorInstance)
				{
					activeChar.sendPacket(new DoorInfo((L2DoorInstance) object));
					activeChar.sendPacket(new DoorStatusUpdate((L2DoorInstance) object));
				}
				else if (object instanceof L2BoatInstance)
				{
					((L2BoatInstance) object).sendBoatInfo(activeChar);
				}
				else if (object instanceof L2StaticObjectInstance)
				{
					activeChar.sendPacket(new StaticObject((L2StaticObjectInstance) object));
				}
				else if (object instanceof L2NpcInstance)
				{
					if (((L2NpcInstance) object).getRunSpeed() == 0)
					{
						activeChar.sendPacket(new ServerObjectInfo((L2NpcInstance) object, activeChar));
					}
					else
					{
						activeChar.sendPacket(new NpcInfo((L2NpcInstance) object, activeChar));
					}
				}
				else if (object instanceof L2Summon)
				{
					L2Summon summon = (L2Summon) object;
					
					// Check if the L2PcInstance is the owner of the Pet
					if (activeChar.equals(summon.getOwner()))
					{
						summon.broadcastStatusUpdate();
						
						if (summon instanceof L2PetInstance)
						{
							activeChar.sendPacket(new PetItemList((L2PetInstance) summon));
						}
					}
					else
					{
						activeChar.sendPacket(new NpcInfo(summon, activeChar, 1));
					}
				}
				else if (object instanceof L2PcInstance)
				{
					L2PcInstance otherPlayer = (L2PcInstance) object;
					
					if (otherPlayer.isInBoat())
					{
						otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition().getWorldPosition());
						activeChar.sendPacket(new CharInfo(otherPlayer));
						
						int relation = otherPlayer.getRelation(activeChar);
						
						if (otherPlayer.getPet() != null)
						{
							activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, activeChar.isAutoAttackable(otherPlayer)));
						}
						
						if ((otherPlayer.getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null) && (otherPlayer.getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation))
						{
							activeChar.sendPacket(new RelationChanged(otherPlayer, relation, activeChar.isAutoAttackable(otherPlayer)));
							if (otherPlayer.getPet() != null)
							{
								activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, activeChar.isAutoAttackable(otherPlayer)));
							}
						}
						activeChar.sendPacket(new GetOnVehicle(otherPlayer.getObjectId(), otherPlayer.getBoat().getObjectId(), otherPlayer.getInBoatPosition()));
					}
					else
					
					{
						activeChar.sendPacket(new CharInfo(otherPlayer));
						
						int relation = otherPlayer.getRelation(activeChar);
						
						if (otherPlayer.getPet() != null)
						{
							activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, activeChar.isAutoAttackable(otherPlayer)));
						}
						
						if ((otherPlayer.getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null) && (otherPlayer.getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation))
						{
							activeChar.sendPacket(new RelationChanged(otherPlayer, relation, activeChar.isAutoAttackable(otherPlayer)));
							
							if (otherPlayer.getPet() != null)
							{
								activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, activeChar.isAutoAttackable(otherPlayer)));
							}
						}
					}
				}
				
				if (object instanceof L2Character)
				{
					// Update the state of the L2Character object client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance
					L2Character obj = (L2Character) object;
					if (obj.getAI() != null)
					{
						obj.getAI().describeStateToPlayer(activeChar);
					}
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _0__CF_REQUEST_RECORD_INFO;
	}
}