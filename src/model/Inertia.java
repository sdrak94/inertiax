package inertiax.model;

import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__ASSIST_NO_TARGET;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__ATK_TARGET;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__CREDIT_END;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__END;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__FOLLOW_CLOSE;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__FOLLOW_FAR;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__NEW_TARGET;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__NO_TARGET;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__ON_KILL;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__REM_TARGET;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__START;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__WHILE_DEAD;
import static inertiax.enums.IInertiaCmd.EInertiaEvt.EVT__WHILE_TARGET_DEAD;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import drake.aepvp.authentication.PlayerPassport;
import drake.aepvp.model.controlers.GlobalRadar;
import ghosts.model.Ghost;
import inertiax.controller.InertiaController;
import inertiax.enums.EActionPriority;
import inertiax.enums.EAutoAttack;
import inertiax.enums.EMoveType;
import inertiax.enums.EPanelOption;
import inertiax.enums.ESearchType;
import inertiax.enums.IInertiaCmd.EInertiaEvt;
import inertiax.model.ext.InertiaExt;
import inertiax.model.filters.ActionFilter;
import inertiax.model.filters.AvailSkillActionFilter;
import inertiax.model.filters.TargetFilter;
import inertiax.model.filters.VTargetFilter;
import inertiax.model.panels.DropTracker;
import inertiax.model.panels.TargetFiltering;
import l2.ae.pvp.gameserver.datatables.SkillTable;
import l2.ae.pvp.gameserver.model.ILocational;
import l2.ae.pvp.gameserver.model.L2Skill;
import l2.ae.pvp.gameserver.model.Location;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.serverpackets.ExServerPrimitive;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;
import l2.ae.pvp.gameserver.util.Util;

public class Inertia implements Callable<Inertia>
{
	private final int _ownerId;

	private final long INIT_TICKS = 4 * 3_600_000;

//	private final IInertiaBehave _behave;
	private final InertiaAct _inertiaAct = new InertiaAct(this);
	
	private final PlayerPassport _playerPassport;

	private boolean _running;
	private long _lagTicks;
	private long _remainingTicks = INIT_TICKS;
	private EAutoAttack _autoAttack;
	private EMoveType _moveType;
	private ESearchType _searchType;
	private PlayerPassport _assistPassport;
	private Location _lastSavedLocation;
	private final InertiaCast[] _inertiaSkills = new InertiaCast[7];
	private final TargetComparator targetComperator = new TargetComparator();
	private final TargetFilter _targetFilter = new TargetFilter(this);
	private final VTargetFilter _vtargetFilter = new VTargetFilter(this);
	private final ActionFilter actionFilter = new ActionFilter(this);
	private final AvailSkillActionFilter availSkillFilter = new AvailSkillActionFilter(this);

	private boolean _exit;

	private final HashMap<EPanelOption, InertiaPanel> _panels = new HashMap<>();

	@Override
	public Inertia call()
	{
		if (_running)
		{
			tick(InertiaController.TICKS);
			tickEnd();
			_exit = false;
		}
		return this;
	}
	
	public InertiaAct getInertiaAct()
	{
		return _inertiaAct;
	}

	public Inertia(final PlayerPassport playerPassport, final long remainingTicks)
	{
		_ownerId = playerPassport.getObjectId();
		_playerPassport = playerPassport;
		if (remainingTicks > 0)
			_remainingTicks = remainingTicks;
		else
			_remainingTicks = INIT_TICKS;

		reset();
	}

	public InertiaCast setInertiaAction(int slot, int actionId, boolean isSkill)
	{
		final var inertiaSlots = _inertiaSkills;
		final int slotsLen = inertiaSlots.length;
		if (slot >= slotsLen)
			return null;
		return inertiaSlots[slot] = new InertiaCast(actionId, isSkill);
	}

	public InertiaCast getInertiaAction(int slot, final boolean isSkill)
	{
		final var inertiaSlots = _inertiaSkills;
		if (slot < 0 || slot >= inertiaSlots.length)
			return null;
		return inertiaSlots[slot];
	}

	public boolean swapInertiaAction(final int slot0, final int slot1, final boolean isSkill)
	{
		final var inertiaSlots = _inertiaSkills;
		if (slot0 < 0 || slot0 >= inertiaSlots.length)
			return false;
		if (slot1 < 0 || slot1 >= inertiaSlots.length)
			return false;
		final var inertAction = inertiaSlots[slot0];
		inertiaSlots[slot0] = inertiaSlots[slot1];
		inertiaSlots[slot1] = inertAction;
		return true;
	}

	public void addLag(final long newLag)
	{
		_lagTicks += newLag;
	}

	public void deleteInertiaAction(final int slot0, final boolean isSkill)
	{
		final var inertiaSlots = _inertiaSkills;
		inertiaSlots[slot0] = null;
	}

	public void addCredit(final long ticks)
	{
		_remainingTicks += ticks;
	}

	public long getCredit()
	{
		return _remainingTicks;
	}

	public void turnOff()
	{
		setRunning(false);
		render();
		return;
	}

	public boolean exit()
	{
		return _exit;
	}

	public void raiseExit()
	{
		_exit = true;
	}

	public void tick(final long ticks)
	{
		final var player = getActivePlayer();

		if (player == null)
		{
			setRunning(false);
			return;
		}

		if (player.isDead())
		{
			evt(EVT__WHILE_DEAD);
			if (exit())
				return;
		}
		if (_remainingTicks - ticks < 0 || player == null)
		{
			_remainingTicks = 0;
			evt(EVT__CREDIT_END);
			if (exit())
				return;
		}
//		_remainingTicks -= ticks;
		if (_lagTicks > 0)
		{
			_lagTicks = Math.max(0, _lagTicks - ticks);
			return;
		}
		
		_inertiaAct.tick(ticks);
		_inertiaAct.evt(EVT__START);
		
		// final var oldTarget = player.getTarget();
		// if (! (player.getTarget() instanceof L2Character))
		// return;
		final L2Character oldTarget = (L2Character) player.getTarget();
		if (oldTarget != null && oldTarget.isAlikeDead())
		{
			evt(EVT__REM_TARGET);
//			addLag(1000);
			return;
		}
		final var party = player.getParty();
		final var assistPlayer = getAssistPlayer();
		if (assistPlayer != null && !assistPlayer.isSamePartyWith(player))
			_assistPassport = null;

		if (party == null || _assistPassport == null)
		{
			boolean render = false;
			if (_moveType == EMoveType.Follow_Target)
			{
				setMoveType(EMoveType.Not_Set);
				render = true;
			}
			if (_searchType == ESearchType.Assist)
			{
				setSearchTarget(ESearchType.Off);
				render = true;
			}
			if (render)
				render();
		}

		final L2Character currTarget = player.getTargetChar();

		if (assistPlayer != null)
		{
			if (_moveType == EMoveType.Follow_Target && !player.isMoving())
			{
				if (player.isInsideRadius(assistPlayer, 500, false) || player.isInCombat())
				{
					evt(EVT__FOLLOW_CLOSE);
				} 
				else
				{
					evt(EVT__FOLLOW_FAR);
				}
			}
			if (currTarget == null)
			{
				evt(EVT__ASSIST_NO_TARGET);
			}
		}

		if (_searchType != ESearchType.Off)
		{
			if (_moveType == EMoveType.Not_Set)
				renderRange();
			if (currTarget != null && currTarget.isAlikeDead())
			{
				evt(EVT__WHILE_TARGET_DEAD);
			} 
			else if (currTarget == null || (currTarget == player) || _searchType == ESearchType.Assist)
			{
				final L2Character newTarget = searchTarget();
				if (newTarget != null && newTarget != currTarget)
				{
					_inertiaAct.setTarget(newTarget);
					evt(EVT__NEW_TARGET);
					return;
				}
			}
		}
		L2Character actualTarget = player.getTargetChar();
		if (actualTarget == null)
		{
			evt(EVT__NO_TARGET);
			actualTarget = _inertiaAct.getTarget();
		}

		if (actualTarget == null)
			return;
		
//		final L2Character playerTarget = actualTarget.getActingPlayer();
//		 if (playerTarget != null && playerTarget.isSameHWID(player) && playerTarget != player)
//		 {
//			 player.sendMessage("Its not allowed to auto chill targets from the same IP!");
//			 setRunning(false);
//			 render();
//		 }
		if (actualTarget.isAutoAttackable(player) && forceAutoAttack())
		{
			startAutoAttack(actualTarget);
			return;
		}
		else
		{
			final var avail = getAvailSkillActions().filter(availSkillFilter).findFirst();
			if (avail != null && avail.isPresent())
			{
				final InertiaCast availCast = avail.get();
				if (availCast != null)
				{

					final L2Skill availSkill = getSkill(availCast);

					if (availSkill != null)
					{
						if (availSkill.isOffensive() && !actualTarget.isAutoAttackable(player))
							player.setTarget(null);
							
						if (player.testDoCastConditions(availSkill))
						{
							if (player.useMagic(availSkill, false, false))
							{
								availCast.initReuse();
								return;
							}

						}
						if (_autoAttack != EAutoAttack.Never)
						{
							startAutoAttack(actualTarget);
							return;
						}
					}
				}
			} 
			else if (_autoAttack == EAutoAttack.Skills_Reuse)
			{
				startAutoAttack(actualTarget);
				return;
			}
		}
	}
	
	public L2Skill getSkill(final InertiaCast inertiaCast)
	{
		final L2PcInstance activePlayer = getActivePlayer();
		if (activePlayer == null)
			return null;
		
		final int skillLvl = activePlayer.getSkillLevel(inertiaCast.getActionId());
		return SkillTable.getInstance().getInfo(inertiaCast.getActionId(), skillLvl);
	}

	private boolean forceAutoAttack()
	{
		if (_autoAttack == EAutoAttack.Always)
			return true;
		else if (_autoAttack == EAutoAttack.Skills_Reuse && cantAction())
			return true;

		return false;
	}

	private boolean cantAction()
	{
		final var player = getActivePlayer();
		for (final var inertiaSkill : _inertiaSkills)
		{
			if (inertiaSkill != null && !inertiaSkill.isUsableNow(player))
				return false;
		}

		return false;
	}

	public void tickEnd()
	{
		evt(EVT__END);
	}

	public void onLogout()
	{
		_running = false;
	}

	public void onKill(final L2Character victim)
	{
		//addLag(700);
		evt(EVT__ON_KILL);
	}

	private void startAutoAttack(final L2Character actualTarget)
	{
		_inertiaAct.setTarget(actualTarget);
		evt(EVT__ATK_TARGET);
	}

	private Stream<InertiaCast> getAvailSkillActions()
	{
		return Stream.of(_inertiaSkills).filter(actionFilter);
	}

	public L2Character searchTarget()
	{

		switch (_searchType)
		{
		case Assist:
			return getTargetByAssist();
		}

		final var target = _inertiaAct.getTarget();
		if (target != null)
			return target;

		return getTargetByRange(_searchType.getRange());
	}

	public L2Character getTargetByAssist()
	{
		final var assistPlayer = getAssistPlayer();
		if (assistPlayer == null)
			return null;
		return assistPlayer.getTargetChar();
	}

	public L2Character getTargetByRange(final int range)
	{
		final var player = getActivePlayer();

		return player.getKnownList().getKnownCharacters().stream().filter(_targetFilter).sorted(targetComperator)

				.findFirst().orElse(null);
	}

	public ILocational getSearchLocation()
	{
		final var loc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : getActivePlayer();
		return loc;
	}

	private class TargetComparator implements Comparator<L2Character>
	{
		@Override
		public int compare(L2Character o1, L2Character o2)
		{
			final var loc = getSearchLocation();
			final double d1 = GlobalRadar.getRadarDistance(loc, o1);
			final double d2 = GlobalRadar.getRadarDistance(loc, o2);

			return Double.compare(d1, d2);
		}
	}

	public L2PcInstance getActivePlayer()
	{
		final var player = _playerPassport.getOnlinePlayer();
		return player;
	}

	public L2PcInstance getAssistPlayer()
	{
		if (_assistPassport == null)
			return null;
		return _assistPassport.getOnlinePlayer();
	}

	public void reset()
	{
		_running = false;
		_autoAttack = EAutoAttack.Never;
		_searchType = ESearchType.Off;
		_moveType = EMoveType.Not_Set;
		_assistPassport = null;
		_lastSavedLocation = null;
		Util.clearArray(_inertiaSkills);
		renderRange();
	}

	public void setPartyTarget(final PlayerPassport targetPassport)
	{
		if (targetPassport == _assistPassport || targetPassport == _playerPassport)
			return;
		final var player = getActivePlayer();
		if (targetPassport != null)
		{
			final var targetPlayer = targetPassport.getPlayer();
			if (targetPlayer == null)
				return;
			if (player.isSameHWID(targetPlayer))
			{
				_assistPassport = null;
				player.sendMessage("Same IP inertia is prohibited and a bannable offense.");
				return;
			}
		}
		_assistPassport = targetPassport;
		if (_assistPassport == null)
		{
			player.sendMessage("InertiaMode PartyTarget changed to -> UNSET");
			if (_moveType == EMoveType.Follow_Target)
				setMoveType(EMoveType.Not_Set);
		} else
			player.sendMessage("InertiaMode PartyTarget changed to -> [" + _assistPassport.getPlayerName() + "]");
	}

	public void setAutoAttack(final EAutoAttack autoAttack)
	{
		if (autoAttack == _autoAttack)
			return;
		_autoAttack = autoAttack;
		final var player = getActivePlayer();
		player.sendMessage("InertiaMode AttackType changed to -> [" + _autoAttack + "]");
	}

	public void setMoveType(EMoveType moveType)
	{
		final var player = getActivePlayer();
		if (moveType == EMoveType.Current_Location)
		{
			_lastSavedLocation = new Location(player.getLocation());
			player.sendMessage("Updated search location to current position.");
			moveType = EMoveType.Saved_Location;
		}
		renderRange();
		if (moveType == _moveType)
			return;
		_moveType = moveType;
		player.sendMessage("InertiaMode MoveType changed to -> [" + _moveType + "]");
		renderRange();
	}

	public void setSearchTarget(final ESearchType searchType)
	{
		if (searchType == _searchType)
			return;
		_searchType = searchType;
		final var player = getActivePlayer();
		player.sendMessage("InertiaMode SearchType changed to -> [" + _searchType + "]");
		renderRange();
	}

	private static final String STOPPED = "<td align=center><button value=\"Start\" action=\"bypass inertia_start %d\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=center><font name=hs12 color=\"FF6363\">Stopped</font></td>";
	private static final String RUNNING = "<td align=center><font name=hs12 color=\"63FF63\">Running</font></td><td align=center><button value=\"Stop\" action=\"bypass inertia_stop %d\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";

	public void render(L2PcInstance viewer)
	{
		final var player = getActivePlayer();
		if (player != null)
		{
			final var npcHtml = new NpcHtmlMessage();
			npcHtml.setFile(player, "data/html/aepvp/inertiax/main.htm");
			// state

			final String running = String.format(_running ? RUNNING : STOPPED, _ownerId);

			npcHtml.replace("%state%", running);
			npcHtml.replace("%attack%", buildAutoAttack());
			npcHtml.replace("%move%", buildMoveType());
			npcHtml.replace("%party%", buildParty());
			npcHtml.replace("%opt%", buildOptions());
			npcHtml.replace("%search%", buildSearch());
			npcHtml.replace("%time%", buildTime());
			npcHtml.replace("%ask%", buildActions(_inertiaSkills));

			npcHtml.replace("%id%", _ownerId);
			npcHtml.replace("%name%", player.getName());
			viewer.sendPacket(npcHtml);
		}
	}

	public void render()
	{
		final var player = getActivePlayer();
		if (player != null)
		{
			render(player);
		}
	}

	private String buildTime()
	{
		final long hours = _remainingTicks / 3_600_000;
		final long minutes = (_remainingTicks - (3_600_000 * hours)) / 60_000;
		final long seconds = _remainingTicks - hours * 3_600_000 - minutes * 60_000;
		return String.format("%02d Hours %02d Minutes %02d Seconds", hours, minutes, seconds / 1000);
	}

	private String buildAutoAttack()
	{
		final var eattackTypes = EAutoAttack.values();
		final List<EAutoAttack> attackTypes = new ArrayList<>(eattackTypes.length);
		for (final var eattackType : eattackTypes)
			if (eattackType != _autoAttack)
				attackTypes.add(eattackType);
		String ret = _autoAttack.toString();
		for (final var attackType : attackTypes)
			ret += ";" + attackType;
		return ret;
	}

	private String buildMoveType()
	{
		final var emoveTypes = EMoveType.values();
		final List<EMoveType> moveTypes = new ArrayList<>(emoveTypes.length);
		for (final var emoveType : emoveTypes)
			if (emoveType != _moveType)
				moveTypes.add(emoveType);
		String ret = _moveType.toString();
		for (final var moveType : moveTypes)
		{
			if (moveType == EMoveType.Saved_Location && _lastSavedLocation == null)
				continue;
			if (moveType != EMoveType.Follow_Target || _assistPassport != null)
				ret += ";" + moveType;
		}
		ret = ret.replace("Target", _assistPassport == null ? "Target" : _assistPassport.getPlayerName());
		return ret;
	}

	private String buildParty()
	{
		final var player = getActivePlayer();
		final var party = player.getParty();
		if (party == null)
			return "Not Set";
		String ret = _assistPassport == null ? "Not Set" : _assistPassport.getPlayerName() + ";Not Set";
		for (final var member : party.getPartyMembers())
		{
			final var memberPassport = member.getPassport();
			if (memberPassport != _assistPassport && memberPassport != _playerPassport)
				ret += ";" + member.getName();
		}
		return ret;
	}
	
	private static final String search = "<td align=center width=50><button value=\"%s\" action=\"bypass inertia_search_type %d %s\" width=62 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";
	private static final String searcs = "<td align=center width=50><font name=hs12 color=\"%s\">%s</font></td>";

	private String buildSearch()
	{
		final StringBuilder sb = new StringBuilder(512);
		for (final var esearch : ESearchType.values())
		{
			if (esearch == _searchType)
				sb.append(String.format(searcs, esearch.getColor(), esearch.toString(), esearch.toString()));
			else
				sb.append(String.format(search, esearch.toString(), _ownerId, esearch.toString()));
		}
		return sb.toString();
	}

	private String buildOptions()
	{
		String opts = "";
		for (final var opt : EPanelOption.values())
			opts += opt.toString() + ";";
		return opts;
	}

	private static final String actionTemplate = "<td align=center width=50><table height=34 cellspacing=0 cellpadding=0 background=%s><tr><td><table cellspacing=0 cellpadding=0><tr><td><button action=\"bypass inertia_action_edit %d %s\" width=34 height=34 back=L2UI_CH3.menu_outline_Down fore=L2UI_CH3.menu_outline></td></tr></table></td></tr></table></td>";

	private String buildActions(final InertiaCast[] inertiaActions)
	{
		final StringBuilder sb = new StringBuilder(1024);
		int aid = 0;
		for (final var inertiaAction : inertiaActions)
		{
			if (inertiaAction != null)
				sb.append(String.format(actionTemplate, inertiaAction.getIcon(), _ownerId, String.valueOf(aid++)));
			else
				sb.append(String.format(actionTemplate, "L2UI_CT1.Inventory_DF_CloakSlot_Disable", _ownerId, String.valueOf(aid++)));
		}
		return sb.toString();
	}

	public boolean toggleFilteredTarget(final L2PcInstance viewer, final int npcTemplateId)
	{
		final var panel = fetchPanel(EPanelOption.Target_Filter);
		if (panel instanceof TargetFiltering targetFiltering)
			return targetFiltering.toggleFilteredId(npcTemplateId);
		return false;
	}

	public void renderActionEdit(final int slot, final int page)
	{
		final int SKILLS_PER_PAGE = 9;
		final var npcHtml = new NpcHtmlMessage();
		npcHtml.setFile(getActivePlayer(), "data/html/aepvp/inertiax/skill.htm");
		final StringBuilder sb = new StringBuilder();
		npcHtml.replace("%tit%", "Inertia Action " + slot);
		final var player = getActivePlayer();
		final ArrayList<L2Skill> availSkills = new ArrayList<>();
		for (final var skill : player.getAllSkills())
		{
			if (skill.isActive() && !skill.isToggle() && skill.isInertiaAllow())
				availSkills.add(skill);
		}
		final int skillsLen = availSkills.size();
		for (int i = 0; i < SKILLS_PER_PAGE; i++)
		{
			final int indx = SKILLS_PER_PAGE * page + i;
			if (indx < skillsLen)
			{
				final var skill = availSkills.get(indx);
				sb.append(String.format(actionTemplate.replace("inertia_action_edit", "inertia_action_set"), skill.getIcon(), _ownerId, slot + " " + skill.getId()));
			}
		}
		npcHtml.replace("%ask%", sb.toString());
		// pages
		final int pages = skillsLen < SKILLS_PER_PAGE ? 1 : skillsLen / SKILLS_PER_PAGE + ((skillsLen % SKILLS_PER_PAGE) > 0 ? 1 : 0);
		sb.setLength(0);
		for (int i = 0; i < pages; i++)
		{
			if (page == i)
				sb.append(String.format("<td align=center>Page %d</td>", i + 1));
			else
				sb.append(String.format("<td align=center><a action=\"bypass inertia_action_edit %d %d %d\">Page %d</a></td>", _ownerId, slot, i, i + 1));
		}
		npcHtml.replace("%pages1%", sb.toString());
		//
		final var action = _inertiaSkills[slot];
		if (action != null)
		{
			final var skill = SkillTable.getInstance().getInfoLevelMax(action.getActionId());
			npcHtml.replace("%sic%", skill.getIcon());
			npcHtml.replace("%sna%", skill.getName());
			npcHtml.replace("%reu%", String.format("%.2fs", action.getReuse()));
			npcHtml.replace("%hpp%", String.format("%05.2f%%", action.getUserHp()));
			npcHtml.replace("%tpp%", String.format("%05.2f%%", action.getTargetHp()));
			final var epriorities = EActionPriority.values();
			final var priority = epriorities[slot];
			String spr = priority.toString();
			for (final var pr : epriorities)
				if (pr != priority)
					spr += ";" + pr.toString();
			npcHtml.replace("%pr%", spr);
		} else
		{
			npcHtml.replace("%sic%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
			npcHtml.replace("%sna%", "Empty");
			npcHtml.replace("%reu%", "?");
			npcHtml.replace("%hpp%", "?");
			npcHtml.replace("%tpp%", "?");
			npcHtml.replace("%pr%", "");
		}
		npcHtml.replace("%priority%", slot + 1);
		npcHtml.replace("%slot%", slot);
		npcHtml.replace("%id%", _ownerId);
		player.sendPacket(npcHtml);
	}

	private void renderRange()
	{
		final var player = getActivePlayer();
		if ((player instanceof Ghost) || player == null)
			return;
		final int searchRange = _searchType.getRange();
		final ILocational renderLoc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : player;
		final ExServerPrimitive renderRange = new ExServerPrimitive("SearchRange", renderLoc);
		if ((_moveType != EMoveType.Follow_Target) && (searchRange > 1))
		{
			final var color = _running ? Color.GREEN : Color.RED;
			renderRange.addCircle(color, searchRange, 30, -20);
			renderRange.addCircle(color, 5, 4, -20);
		} else
			renderRange.addCircle(Color.GREEN, 1, 1, -5);
		player.sendPacket(renderRange);
	}

	public boolean isRunning()
	{
		return _running;
	}

	public void setRunning(final boolean running)
	{
		_running = running;
		renderRange();
	}

	public L2Character getTargetAssist()
	{
		return null;
	}

	public L2Character getTargetRange(final int range)
	{
		return null;
	}

	public ESearchType getSearchType()
	{
		return _searchType;
	}

	public VTargetFilter getVTargetFilter()
	{
		return _vtargetFilter;
	}

	public int getOwnerId()
	{
		return _ownerId;
	}

	public void onItemDrop(final int itemId, final long count)
	{
		if (_running)
		{
			final var inertiaPanel = fetchPanel(EPanelOption.Drops_Tracker);
			if (inertiaPanel instanceof DropTracker dropTracker)
				dropTracker.insertDrop(itemId, count);
		}
	}

	public void renderPanel(final EPanelOption panelOption, final L2PcInstance viewer)
	{
		final var panel = fetchPanel(panelOption);
		panel.render(viewer);
	}

	public InertiaPanel getPanel(final EPanelOption panelOption)
	{
		return _panels.get(panelOption);
	}

	public InertiaPanel fetchPanel(final EPanelOption panelOption)
	{
		return _panels.computeIfAbsent(panelOption, p -> p.getCompute().apply(this));
	}

	private void evt(final EInertiaEvt inertiaEvt)
	{
		_inertiaAct.evt(inertiaEvt);
	}

	public void addInertiaExt(final InertiaExt inertiaExt)
	{
		_inertiaAct.addInertiaExt(inertiaExt);
	}
}
