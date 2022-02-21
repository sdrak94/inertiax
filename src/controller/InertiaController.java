package inertiax.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import drake.aepvp.authentication.PassportManager;
import drake.aepvp.authentication.PlayerPassport;
import drake.aepvp.model.controlers.RealTimeController;
import drake.aepvp.model.interfaces.ITimeTrigger;
import gnu.trove.map.hash.TIntLongHashMap;
import inertiax.enums.EActionPriority;
import inertiax.enums.EAutoAttack;
import inertiax.enums.EMoveType;
import inertiax.enums.EPanelOption;
import inertiax.enums.ESearchType;
import inertiax.model.Inertia;
import inertiax.model.InertiaAct;
import l2.ae.pvp.L2DatabaseFactory;
import l2.ae.pvp.gameserver.Shutdown;
import l2.ae.pvp.gameserver.ThreadPoolManager.PriorityThreadFactory;
import l2.ae.pvp.gameserver.model.IBypassHandler;
import l2.ae.pvp.gameserver.model.IStorable;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.clientpackets.RequestBypassToServer;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;

public class InertiaController implements IBypassHandler, IStorable, ITimeTrigger
{
	public static long TICKS = 800;
	
	private final TIntLongHashMap _playerCredit = new TIntLongHashMap();
	
	private final ConcurrentHashMap<PlayerPassport, Inertia> _playerInertias = new ConcurrentHashMap<>();
	
	private static final ScheduledExecutorService INERTIA_MAIN = Executors.newSingleThreadScheduledExecutor();
	private static final ThreadPoolExecutor       INERTIA_POOL = new ThreadPoolExecutor(4, 6, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("INERTIAX Pool", Thread.NORM_PRIORITY)); 

	private final InertiaTask _inertiaTask = new InertiaTask();
	
	private InertiaController()
	{
		RequestBypassToServer.register(this);
		Shutdown.getInstance().addShutdownHook(this);
		RealTimeController.registerHook(this);
		
		_inertiaTask.execute(1000);
		
		load();
	}
	
	public void stop()
	{
		_inertiaTask.stop();
	}
	
	public void start()
	{
		_inertiaTask.start();
	}
	
	public boolean isRunning()
	{
		return _inertiaTask.isRunning();
	}
	
	public void setTicks(final long ticks)
	{
		TICKS = ticks;
	}
	
	public long getLag()
	{
		return _inertiaTask.getLag();
	}
	
	private class InertiaTask implements Runnable
	{
		private long _lag;
		
		private boolean _running = true;
		
		private final List<Callable<Inertia>> callables = new ArrayList<>(100);
		
		@Override
		public void run()
		{
			final long t0 = System.nanoTime();
			
			callables.addAll(_playerInertias.values());
			
			if (callables.size() > 0) try
			{
				INERTIA_POOL.invokeAll(callables, TICKS, TimeUnit.MILLISECONDS);
				callables.clear();
			}
			catch (InterruptedException e)
			{
				stop();
				e.printStackTrace();
			}

			_lag = System.nanoTime() - t0;
			final long delay = TICKS - TimeUnit.NANOSECONDS.toMillis(_lag);
			execute(delay);
			

//			System.out.println(String.format("%.02f", _lag / 1000_000d));
		}
		
		public long getLag()
		{
			return _lag;
		}
		
		public void execute(final long delay)
		{
			if (isRunning())
				INERTIA_MAIN.schedule(this, delay, TimeUnit.MILLISECONDS);
		}
		
		public void stop()
		{
			_running = false;
		}
		
		public void start()
		{
			if (_running)
				throw new RuntimeException("Already Running!");
			
			_running = true;
			execute(0);
		}
		
		private boolean isRunning()
		{
			return _running;
		}
	}
	
	private final String SET_ON = "<tr><td><font name=hs12 color=\"LEVEL\">InertiaX Core</font></td><td align=center><font name=hs12 color=\"63FF63\">Active</font></td><td align=center><button value=\"Shutdown\" action=\"bypass admin_inertia_shutdown\" width=80 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>";
	private final String SET_OF = "<tr><td><font name=hs12 color=\"LEVEL\">InertiaX Core</font></td><td align=center><button value=\"Active\" action=\"bypass admin_inertia_activate\" width=80 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=center><font name=hs12 color=\"FF6363\">Shutdown</font></td></tr>";

	public void adminPanel(final L2PcInstance player)
	{
		final var npcHtml = new NpcHtmlMessage();
		npcHtml.setFile(player, "data/html/aepvp/inertiax/admin.htm");
		
		
		npcHtml.replace("%state%", isRunning() ? SET_ON : SET_OF);

		final var list = new ArrayList<>(Arrays.asList(300L, 500L, 600L, 800L, 900L, 1300L, 1600L, 1800L, 2200L, 2600L, 3300L, 3800L, 4400L, 5500L, 6200L));
		list.remove(Long.valueOf(TICKS));
		
		String ticks = String.valueOf(TICKS);
		for (final var tick : list)
			ticks += ";" + tick;
		
		npcHtml.replace("%ticks%", ticks);

		npcHtml.replace("%lag%", String.format("%.02f", getLag() / 1000_000d));
		

		npcHtml.replace("%count%", String.format("%d / %d", _playerInertias.values().stream().filter(Inertia::isRunning).count(), _playerInertias.size()));
		
		player.sendPacket(npcHtml);
	}
	
	public Inertia fetchInertia(final L2PcInstance player)
	{
		final var playerPassport = player.getPassport();
		
		return fetchInertia(playerPassport);
	}
	
	public Inertia fetchInertia(final PlayerPassport playerPassport)
	{
		var inertia = _playerInertias.get(playerPassport);
		
		if (inertia == null)
		{
			final var player = playerPassport.getPlayer();
			
			inertia = new Inertia(playerPassport, _playerCredit.get(playerPassport.getObjectId()));
			final InertiaAct inertiaAct = inertia.getInertiaAct();
			player.processInertiaAct(inertiaAct);
			
			_playerInertias.put(playerPassport, inertia);
		}
		
		return inertia;
	}
	
	public Inertia findInertia(final L2PcInstance player, final int viewId)
	{
		final var passport = PassportManager.getInstance().getById(viewId);
		
		if (player.getPassport() != passport && player.isGM())
			return fetchInertia(passport);
		
		return fetchInertia(player);
			
		
	}
	
	public Inertia getInertia(final L2PcInstance player)
	{
		return _playerInertias.get(player.getPassport());
	}

	@Override
	public boolean handleBypass(L2PcInstance player, String cmd)
	{
		if (!cmd.contains("inertia") || cmd.contains("admin"))
			return false;
		
		final StringTokenizer st = new StringTokenizer(cmd);
		st.nextToken();
		
		if (!st.hasMoreTokens())
			return false;
		
		final int viewId = Integer.parseInt(st.nextToken());

		final var inertia = findInertia(player, viewId);
		
		if (cmd.startsWith("inertia_start"))
		{
			inertia.setRunning(true);
			inertia.render(player);
			return true;
		}
		else if (cmd.startsWith("inertia_stop"))
		{
			inertia.setRunning(false);
			inertia.render(player);
			return true;
		}
		else if (cmd.startsWith("inertia_reset"))
		{
			inertia.reset();
			inertia.render(player);
			return true;
		}
		else if (cmd.startsWith("inertia_refresh") || cmd.startsWith("inertia_main"))
		{
			inertia.render(player);
	
			return true;
		}
		else if (cmd.startsWith("inertia_attack_type"))
		{
			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EAutoAttack attackType = Enum.valueOf(EAutoAttack.class, strType);
				inertia.setAutoAttack(attackType);
				inertia.render(player);
			}
		}
		else if (cmd.startsWith("inertia_move_type"))
		{
			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EMoveType attackType =  strType.contains("Follow") ? EMoveType.Follow_Target : Enum.valueOf(EMoveType.class, strType);
				inertia.setMoveType(attackType);
				inertia.render(player);
			}
			
			return true;
		}
		else if (cmd.startsWith("inertia_search_type"))
		{
			if (st.hasMoreTokens())
			{
				final ESearchType searchType = Enum.valueOf(ESearchType.class, st.nextToken());
				inertia.setSearchTarget(searchType);
				inertia.render(player);
			}
			
			return true;
		}
		else if (cmd.startsWith("inertia_party_target"))
		{
			if (st.hasMoreTokens())
			{
				String name = st.nextToken();
				while (st.hasMoreTokens())
					name += "_" + st.nextToken();
				
				final var targetPassport = PassportManager.getInstance().getByName(name);
				
				inertia.setPartyTarget(targetPassport);
				inertia.render(player);
					
			}
			return true;
		}
		else if (cmd.startsWith("inertia_action_edit"))
		{
			final int slot = Integer.parseInt(st.nextToken());
			
			int page = 0;
			if (st.hasMoreTokens())
				page = Integer.parseInt(st.nextToken());

			inertia.renderActionEdit(slot, page);
			return true;
		}
		else if (cmd.startsWith("inertia_action_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final int acid = Integer.parseInt(st.nextToken());
					
					inertia.setInertiaAction(slot, acid, true);

					inertia.renderActionEdit(slot, 0);
				}
			}
			return true;
		}
		else if (cmd.startsWith("inertia_reuse_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double reus = Double.parseDouble(st.nextToken());
					
					final var action = inertia.getInertiaAction(slot, true);
					if (action != null)
						action.setReuse(reus);
					
					inertia.renderActionEdit(slot, 0);
				}
			}
			return true;
		}
		else if (cmd.startsWith("inertia_hpp_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double userHp = Double.parseDouble(st.nextToken());
					
					final var action = inertia.getInertiaAction(slot, true);
					if (action != null)
						action.setUserHP(userHp);
					
					inertia.renderActionEdit(slot, 0);
				}
			}
			return true;
		}
		else if (cmd.startsWith("inertia_tpp_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double targHp = Double.parseDouble(st.nextToken());
					
					final var action = inertia.getInertiaAction(slot, true);
					if (action != null)
						action.setTargetHP(targHp);
					
					inertia.renderActionEdit(slot, 0);
				}
			}
		}
		else if (cmd.startsWith("inertia_slot_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot0 = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final int slot1 = Integer.parseInt(st.nextToken()) - 1;
					
					final var action = inertia.getInertiaAction(slot0, true);
					
					final var newPriority = EActionPriority.values()[slot1];
					if (newPriority == EActionPriority.Remove)
					{
						inertia.deleteInertiaAction(slot0, action.isSkill());
						inertia.render();
					}
					else if (inertia.swapInertiaAction(slot0, slot1, action.isSkill()))
						inertia.renderActionEdit(slot1, 0);
				}
			}
			return true;
		}
		
		
		//MENU
		else if (cmd.startsWith("inertia_render_panel"))
		{
			if (st.hasMoreTokens())
			{
				final int ord = Integer.parseInt(st.nextToken()) - 1;
				final var panelOptions = EPanelOption.values();
				if (ord < panelOptions.length)
				{
					final var panelOption = panelOptions[ord];
					panelOption.render(inertia, player);
				}
			}
			return true;
		}
		else if (cmd.startsWith("inertia_panel"))
		{
			for (final var panelOption : EPanelOption.values())
			{
				final String cmdeq = String.format("inertia_panel_%s", panelOption.toLowerCase());
				if (cmd.startsWith(cmdeq))
				{
					final var inertiaPanel = inertia.fetchPanel(panelOption);
					inertiaPanel.onBypass(player, st);
					break;
				}
			}
			return true;
		}

		return false;
	}
	
	public void renderInertia(final L2PcInstance player)
	{
		final var inertia = fetchInertia(player);
		inertia.render(player);
	}
	
	private static class InstanceHolder
	{
		private static final InertiaController _instance = new InertiaController();
	}
	
	public static InertiaController getInstance()
	{
		return InstanceHolder._instance;
	}

	@Override
	public void exception(Exception e)
	{
		e.printStackTrace();
	}

	@Override
	public boolean storeMe()
	{
		final long t0 = System.currentTimeMillis();
		
		try (final var con = L2DatabaseFactory.getConnectionS();
			 final var pst = con.prepareStatement("INSERT INTO character_inertia_credit (owner_id, credits) VALUES (?, ?) ON DUPLICATE KEY UPDATE credits = ?"))
		{
			con.setAutoCommit(false);
			for (final var inertiaSet : _playerInertias.entrySet())
			{
				final var playerPassport = inertiaSet.getKey();
				final var inertia = inertiaSet.getValue();

				pst.setInt(1, playerPassport.getObjectId());
				pst.setLong(2, inertia.getCredit());
				pst.setLong(3, inertia.getCredit());
				
				pst.addBatch();
				
			}
			
			
			final int total = pst.executeBatch().length;

			con.commit();
			
			final long t1 = System.currentTimeMillis();
			
			System.err.println("Updates " + total + " player Inertia credits in " + (t1 - t0) + " ms!!!" );
			
			return true;
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public void load()
	{
		try (final var con = L2DatabaseFactory.getConnectionS();
			 final var  st = con.createStatement();
			 final var  rs = st.executeQuery("SELECT * FROM character_inertia_credit"))
		{
			while (rs.next())
			{
				final int ownerId = rs.getInt("owner_id");
				final var credit = rs.getLong("credits");
				
				_playerCredit.put(ownerId, credit);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void notify(String dayName, String timeString)
	{
//		if (timeString.equals(Config.DAILY_CREDIT_TIME))
//		{
//			for (final var autoChill : _playerChills.values())
//			{
//				autoChill.addCredit(Config.DAILY_CREDIT);
//				final var player = autoChill.getActivePlayer();
//
//				if (player != null)
//				{
//					player.sendMessage(String.format("You have been rewarded with %.2f hours of daily auto chill credit.", Config.DAILY_CREDIT / 3_600_000D));
//					
//					if(player.isPremium())
//					{
//						autoChill.addCredit(Config.DAILY_CREDIT_PREMIUM_BONUS);
//						player.sendMessage(String.format("You have been rewarded with extra %.2f hours.", Config.DAILY_CREDIT_PREMIUM_BONUS / 3_600_000D));
//					}
//					
//				}
//			}
//		}
	}
	
	

}

