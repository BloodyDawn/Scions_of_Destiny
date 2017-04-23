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

import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ChairSit;
import net.sf.l2j.gameserver.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * @version $Revision: 1.11.2.7.2.9 $ $Date: 2005/04/06 16:13:48 $
 */
public class RequestActionUse extends L2GameClientPacket
{
	private static final String _C__45_REQUESTACTIONUSE = "[C] 45 RequestActionUse";
	private static Logger _log = Logger.getLogger(RequestActionUse.class.getName());
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = (readD() == 1);
		_shiftPressed = (readC() == 1);
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.finest(activeChar.getName() + " request Action use: id " + _actionId + " 2:" + _ctrlPressed + " 3:" + _shiftPressed);
		}
		
		// dont do anything if player is dead
		if (activeChar.isDead() || (activeChar.isFakeDeath() && (_actionId != 0)))
		{
			activeChar.sendPacket(new ActionFailed());
			return;
		}
		
		// don't do anything if player is out of control
		if (activeChar.isOutOfControl())
		{
			activeChar.sendPacket(new ActionFailed());
			return;
		}
		
		L2Summon pet = activeChar.getPet();
		L2Object target = activeChar.getTarget();
		
		if (Config.DEBUG)
		{
			_log.info("Requested Action ID: " + String.valueOf(_actionId));
		}
		
		switch (_actionId)
		{
			case 0:
			{
				if (activeChar.getMountType() != 0)
				{
					break;
				}
				
				if ((target != null) && !activeChar.isSitting() && (target instanceof L2StaticObjectInstance) && (((L2StaticObjectInstance) target).getType() == 1)
					
					&& (CastleManager.getInstance().getCastle(target) != null)
					
					&& activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
				{
					ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
					activeChar.sendPacket(cs);
					activeChar.sitDown();
					activeChar.broadcastPacket(cs);
					break;
				}
				
				if (activeChar.isSitting() || activeChar.isFakeDeath())
				{
					activeChar.standUp();
				}
				else
				{
					if (!activeChar.isPendingSitting())
					{
						if (activeChar.isMoving())
						{
							activeChar.setIsPendingSitting(true);
						}
						else
						{
							if (activeChar.isAttackingDisabled() || activeChar.isImmobilized() || activeChar.isCastingNow())
							{
								return;
							}
							
							activeChar.sitDown();
						}
					}
				}
				
				if (Config.DEBUG)
				{
					_log.fine("new wait type: " + (activeChar.isSitting() ? "SITTING" : "STANDING"));
				}
				break;
			}
			case 1:
			{
				if (activeChar.isRunning())
				{
					activeChar.setWalking();
				}
				else
				{
					activeChar.setRunning();
				}
				
				if (Config.DEBUG)
				{
					_log.fine("new move type: " + (activeChar.isRunning() ? "RUNNING" : "WALKIN"));
				}
				break;
			}
			case 15:
			case 21: // pet follow/stop
			{
				if ((pet != null) && !pet.isMovementDisabled())
				{
					((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
				}
				break;
			}
			case 16:
			case 22: // pet attack
			{
				if ((target != null) && (pet != null) && (pet != target) && !pet.isMovementDisabled())
				{
					if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						// if L2PcInstance is in Olympiad and the match isn't already start, send a Server->Client packet ActionFailed
						activeChar.sendPacket(new ActionFailed());
						return;
					}
					
					if ((activeChar.getAccessLevel() < Config.GM_PEACEATTACK) && activeChar.isInsidePeaceZone(pet, target))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IN_PEACEZONE));
						return;
					}
					
					if (target.isAutoAttackable(activeChar) || _ctrlPressed)
					{

						if (target instanceof L2DoorInstance)
						{
							if (((L2DoorInstance) target).isAttackable(activeChar))
							{
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							}
							
						}
						else if (!pet.isSiegeGolem())
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						
					}
					else
					
					{
						pet.setFollowStatus(false);
						
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
						
					}
				}
				break;
			}
			case 17:
			case 23: // pet - cancel action
			{

				if ((pet != null) && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
				
				break;
			}
			case 19: // pet unsummon
			{
				if (pet != null)
				{
					// returns pet to control item
					if (pet.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.DEAD_PET_CANNOT_BE_RETURNED));
						return;
					}
					
					// if it is a pet and not a summon
					if (pet instanceof L2PetInstance)
					{
						if (!pet.isHungry())
						{
							pet.unSummon(activeChar);
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_RESTORE_HUNGRY_PETS));
						}
					}
					
				}
				break;
			}
			case 38: // pet mount
			{
				activeChar.mountPlayer(pet);
				
				break;
			}
			case 32: // Wild Hog Cannon - Mode Change
			{
				useSkill(4230);
				break;
			}
			case 36: // Soulless - Toxic Smoke
			{
				useSkill(4259);
				break;
			}
			case 37:
			{
				if (activeChar.isAlikeDead())
				{
					sendPacket(new ActionFailed());
					return;
				}
				
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}
				
				if (activeChar.isSitting())
				{
					activeChar.standUp();
				}
				
				if (activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			}
			case 39: // Soulless - Parasite Burst
			{
				useSkill(4138);
				break;
			}
			case 41: // Wild Hog Cannon - Attack
			{
				useSkill(4230);
				break;
			}
			case 42: // Kai the Cat - Self Damage Shield
			{
				useSkill(4378, activeChar);
				break;
			}
			case 43: // Unicorn Merrow - Hydro Screw
			{
				useSkill(4137);
				break;
			}
			case 44: // Big Boom - Boom Attack
			{
				useSkill(4139);
				break;
			}
			case 45: // Unicorn Boxer - Master Recharge
			{
				useSkill(4025, activeChar);
				break;
			}
			case 46: // Mew the Cat - Mega Storm Strike
			{
				useSkill(4261);
				break;
			}
			case 47: // Silhouette - Steal Blood
			{
				useSkill(4260);
				break;
			}
			case 48: // Mechanic Golem - Mech. Cannon
			{
				useSkill(4068);
				break;
			}
			case 51:
			{
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (activeChar.isAlikeDead())
				{
					sendPacket(new ActionFailed());
					return;
				}
				
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}
				
				if (activeChar.isSitting())
				{
					activeChar.standUp();
				}
				
				if (activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			}
			case 52: // unsummon
			{
				if ((pet != null) && (pet instanceof L2SummonInstance))
				{
					pet.unSummon(activeChar);
				}
				break;
			}
			case 53: // move to target
			{
				if ((target != null) && (pet != null) && (pet != target) && !pet.isMovementDisabled())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			}
			case 54: // move to target hatch/strider
			{
				if ((target != null) && (pet != null) && (pet != target) && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				
				break;
			}
			case 96: // Quit Party Command Channel
			{
				_log.info("96 Accessed");
				break;
			}
			case 97: // Request Party Command Channel Info
			
			{
				_log.info("97 Accessed");
				
				break;
			}
			case 1000: // Siege Golem - Siege Hammer
			{
				if (target instanceof L2DoorInstance)
				{
					useSkill(4079);
				}
				break;
			}
			case 1001:
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
			{
				useSkill(4710); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1004: // Wind Hatchling/Strider - Wild Defense
			{
				useSkill(4711, activeChar); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1005: // Star Hatchling/Strider - Bright Burst
			{
				useSkill(4712); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1006: // Star Hatchling/Strider - Bright Heal
			{
				useSkill(4713, activeChar); // TODO use correct skill lvl based on pet lvl
				break;
			}
			case 1007: // Cat Queen - Blessing of Queen
			{
				useSkill(4699, activeChar);
				break;
			}
			case 1008: // Cat Queen - Gift of Queen
			{
				useSkill(4700, activeChar);
				break;
			}
			case 1009: // Cat Queen - Cure of Queen
			{
				useSkill(4701);
				break;
			}
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
			{
				useSkill(4702, activeChar);
				break;
			}
			case 1011: // Unicorn Seraphim - Gift of Seraphim
			{
				useSkill(4703, activeChar);
				break;
			}
			case 1012: // Unicorn Seraphim - Cure of Seraphim
			{
				useSkill(4704);
				break;
			}
			case 1013: // Nightshade - Curse of Shade
			{
				useSkill(4705);
				break;
			}
			case 1014: // Nightshade - Mass Curse of Shade
			{
				useSkill(4706);
				break;
			}
			case 1015: // Nightshade - Shade Sacrifice
			{
				useSkill(4707);
				break;
			}
			case 1016: // Cursed Man - Cursed Blow
			{
				useSkill(4709);
				break;
			}
			case 1017: // Cursed Man - Cursed Strike/Stun
			{
				useSkill(4708);
				break;
			}
			default:
				_log.warning(activeChar.getName() + ": unhandled action type " + _actionId);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor. Target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 */
	private void useSkill(int skillId, L2Object target)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		L2Summon activeSummon = activeChar.getPet();
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendMessage("Cannot use skills while trading.");
			return;
		}
		
		if (activeSummon != null)
		{
			Map<Integer, L2Skill> _skills = activeSummon.getTemplate().getSkills();
			
			if (_skills == null)
			{
				return;
			}
			
			if (_skills.size() == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.SKILL_NOT_AVAILABLE));
				return;
			}
			
			L2Skill skill = _skills.get(skillId);
			if (skill == null)
			{
				if (Config.DEBUG)
				{
					_log.warning("Skill " + skillId + " missing from npcskills.sql for a summon id " + activeSummon.getNpcId());
				}
				return;
			}
			
			activeSummon.setTarget(target);
			activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor. Target is retrieved from owner' target, then validated by overloaded method useSkill(int, L2Character).
	 */
	private void useSkill(int skillId)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		useSkill(skillId, activeChar.getTarget());
	}
	
	@Override
	public String getType()
	{
		return _C__45_REQUESTACTIONUSE;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return true;
	}
}