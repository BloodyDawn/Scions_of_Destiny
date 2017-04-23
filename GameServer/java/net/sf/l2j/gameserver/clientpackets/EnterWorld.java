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

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.serverpackets.Die;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.serverpackets.GameGuardQuery;
import net.sf.l2j.gameserver.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.MagicEffectIcons;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.serverpackets.QuestList;
import net.sf.l2j.gameserver.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.serverpackets.SignsSky;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.UserInfo;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev656 cbdddd
 * <p>
 * @version $Revision: 1.16.2.1.2.7 $ $Date: 2005/03/29 23:15:33 $
 */
public class EnterWorld extends L2GameClientPacket
{
	private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";
	private static Logger _log = Logger.getLogger(EnterWorld.class.getName());

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
			_log.warning("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}

		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if (Config.DEBUG)
			{
				_log.warning("User already exist in OID map! User " + activeChar.getName() + " is a character clone");
			}
		}

		if (activeChar.isGM())

		{
			if (Config.GM_STARTUP_INVULNERABLE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_GODMODE)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invul"))))
			{
				activeChar.setIsInvul(true);
			}

			if (Config.GM_STARTUP_INVISIBLE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_GODMODE)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invisible"))))
			{
				activeChar.getAppearance().setInvisible();
			}

			if (Config.GM_STARTUP_SILENCE && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_MENU)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_silence"))))
			{
				activeChar.setMessageRefusal(true);
			}

			if (Config.GM_STARTUP_AUTO_LIST && ((!Config.ALT_PRIVILEGES_ADMIN && (activeChar.getAccessLevel() >= Config.GM_MENU)) || (Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_gmliston"))))
			{
				GmListTable.getInstance().addGm(activeChar, false);
			}
			else
			{
				GmListTable.getInstance().addGm(activeChar, true);
			}

			if (Config.GM_NAME_COLOR_ENABLED)
			{
				if (activeChar.getAccessLevel() >= 100)
				{
					activeChar.getAppearance().setNameColor(Config.ADMIN_NAME_COLOR);
				}
				else if (activeChar.getAccessLevel() >= 75)
				{
					activeChar.getAppearance().setNameColor(Config.GM_NAME_COLOR);
				}
			}
		}

		if (activeChar.getCurrentHp() < 0.5)
		{
			activeChar.setIsDead(true);
		}

		if (activeChar.getClan() != null)
		{
			if (activeChar.isClanLeader() && (activeChar.getClan().getLevel() > 3))
			{
				SiegeManager.getInstance().addSiegeSkills(activeChar);
			}

			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
				{
					continue;
				}
				if (siege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
				}
				if (siege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
				}
			}
		}

		sendPacket(new UserInfo(activeChar));

		activeChar.getMacroses().sendUpdate();

		sendPacket(new ItemList(activeChar, false));

		sendPacket(new ShortCutInit(activeChar));

		sendPacket(new HennaInfo(activeChar));

		Quest.playerEnter(activeChar);

		activeChar.sendPacket(new QuestList());
		loadTutorial(activeChar);

		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			activeChar.setProtection(true);
		}

		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

		if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
		{
			L2Event.restoreChar(activeChar);
		}
		else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
		{
			L2Event.restoreAndTeleChar(activeChar);
		}

		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			sendPacket(new SignsSky());
		}

		updateLoginEffectIcons(activeChar);

		activeChar.sendPacket(new EtcStatusUpdate(activeChar));

		// Expand Skill

		activeChar.sendPacket(new ExStorageMaxCount(activeChar));

		// Welcome to Lineage II
		sendPacket(new SystemMessage(34));

		if (Config.DISPLAY_SERVER_VERSION)
		{
			if (Config.SERVER_VERSION != null)
			{
				activeChar.sendMessage(getText("TDJKIFNlcnZlciBWZXJzaW9uOg==") + "   " + Config.SERVER_VERSION);
				activeChar.sendMessage(getText("TDJKIFNlcnZlciBCdWlsZCBEYXRlOg==") + " " + Config.SERVER_BUILD_DATE);
			}

		}

		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);

		String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
		if (serverNews != null)
		{
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			htmlMsg.setHtml(serverNews);
			sendPacket(htmlMsg);
		}

		// just in case player gets disconnected
		L2ClassMasterInstance.showQuestionMark(activeChar);

		PetitionManager.getInstance().checkPetitionMessages(activeChar);

		if ((activeChar.getClanId() != 0) && (activeChar.getClan() != null))
		{
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}

		notifyClanMembers(activeChar);

		activeChar.onPlayerEnter();

		if (Olympiad.getInstance().playerInStadium(activeChar))
		{
			activeChar.doRevive();
			if (!activeChar.isGM())
			{
				activeChar.sendMessage("You have been teleported to the nearest town due to being in an Olympiad Stadium.");
			}
		}

		if (activeChar.isAlikeDead())
		{
			// no broadcast needed since the player will already spawn dead to others
			sendPacket(new Die(activeChar));
		}

		if (!activeChar.isGM() && (activeChar.getSiegeState() < 2) && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
		{
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to being in a siege zone.");
		}

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.CLAN_MEMBERSHIP_TERMINATED));
		}

		if (activeChar.getClan() != null)
		{
			// Add message if clanHall not paid. Possibly this is custom...
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			if ((clanHall != null) && !clanHall.getPaid())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
			}
		}

		RegionBBSManager.getInstance().changeCommunityBoard();

		if (Config.GAMEGUARD_ENFORCE)
		{
			activeChar.sendPacket(new GameGuardQuery());
		}
	}

	private void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}

	/**
	 * @param activeChar
	 */
	private void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			SystemMessage msg = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName());

			clan.broadcastToOtherOnlineMembers(msg, activeChar);

			msg = null;

			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}

	private void updateLoginEffectIcons(L2PcInstance activeChar)
	{
		L2Effect[] effects = activeChar.getAllEffects();
		if ((effects != null) && (effects.length > 0))
		{
			MagicEffectIcons mi = new MagicEffectIcons();
			for (L2Effect e : activeChar.getAllEffects())
			{
				if (e == null)
				{
					continue;
				}

				if (e.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME)
				{
					e.exit();
				}
				else if (e.getEffectType() == L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME)
				{
					e.exit();
				}
				else if (e.getSkill().getId() == 4082)
				{
					e.exit();
				}
				else
				{
					if (e.getShowIcon() && e.getInUse())
					{
						e.addIcon(mi);
					}
				}
			}

			if (mi._effects.size() > 0)
			{
				activeChar.sendPacket(mi);
			}
		}
	}

	/**
	 * @param string
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String getText(String string)
	{
		return new String(Base64.getDecoder().decode(string));
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
}