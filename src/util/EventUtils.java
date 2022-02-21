package inertiax.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import drake.aepvp.datatables.EventTemplateTables;
import drake.aepvp.drivers.PlayerDriver;
import drake.aepvp.instance.world.AbstractWorld;
import drake.aepvp.l2event.AbstractEvent;
import drake.aepvp.model.interfaces.IConditional;
import drake.aepvp.model.interfaces.IInfoPackager.IBooleanPackager;
import drake.aepvp.model.interfaces.IInfoPackager.IFloatPackager;
import drake.aepvp.model.interfaces.IInfoPackager.IIntPackager;
import drake.aepvp.model.template.world.EventTemplate;
import drake.aepvp.model.template.world.InstanceTemplate;
import drake.aepvp.model.template.world.WorldTemplate;
import l2.ae.pvp.gameserver.model.L2World;
import l2.ae.pvp.gameserver.model.Location;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.util.Util;
import l2.ae.pvp.util.Rnd;

public class EventUtils 
{
	public static final IBooleanPackager retfalse = (pack) -> {return false;};
	public static final IBooleanPackager rettrue = (pack) -> {return true;};
	public static final IFloatPackager ret1f = (pack) -> {return 1f;};
	public static final IIntPackager ret0 = (pack) -> {return 0;};
	public static final IIntPackager ret1 = (pack) -> {return 1;};
	
	public static String prepareString(String msg, PlayerDriver playerDriver)
	{
		return msg.replaceAll("\\$player", String.valueOf(playerDriver.getPlayerName()))
				  .replaceAll("\\$kills", String.valueOf(playerDriver.getEventKills()))
				  .replaceAll("\\$flags", String.valueOf(playerDriver.getEventScore()))
				  .replaceAll("\\$points", String.valueOf(playerDriver.getEventScore()))
				  .replaceAll("\\$score", String.valueOf(playerDriver.getEventScore()))
				  .replaceAll("\\$deaths", String.valueOf(playerDriver.getEventDeaths()))
				  .replaceAll("\\$top", formatPosition(playerDriver.getEventPosition()));
	}
	
	public static String prepareString(String msg, PlayerDriver playerDriver, InstanceTemplate template)
	{
		msg = msg.replaceAll("\\$name", template.getName());
		return prepareString(msg, playerDriver);
	}
	
	public static <T> boolean contains(final T[] array, final T element)
	{
		if (array == null || element == null)
			return false;
		for (T t : array) if (t == element)
			return true;
		return false;
	}
	
	public static boolean makeEvent(final L2PcInstance caller, final String eventId)
	{
		final EventTemplate template = EventTemplateTables.getInstance().getTemplate(eventId);
		if (template == null)
		{
			if (caller == null)
				System.out.println("Aborting #" + eventId + " creation because its corrupted or it doesnt exist!");
			else
			{
				caller.sendMessage("Aborting #" + eventId + " creation because its corrupted or it doesnt exist!");
				caller.sendMessage("Press //eventinfo to list all available #eventIds");
			}
			return false;
		}
		for (AbstractEvent evt : AbstractEvent.getCurrentEvents())
		{
			final EventTemplate activeTemplate = evt.getTemplate();
			if (activeTemplate.getId().equals(template.getId()))
			{
				if (caller == null)
					System.out.println("Aborting #" + eventId + " creation because its already active!");
				else
					caller.sendMessage("Aborting #" + eventId + " creation because its already active!");
				return false;
			}
			if (Util.checkIfInRange(200, new Location(activeTemplate.getRegistrationLoc()), new Location(template.getRegistrationLoc()), false))
			{
				if (caller == null)
					System.out.println("Aborting #" + eventId + " creation because its registration is very close to #" + activeTemplate.getName() + " 's registration area.");
				else
					caller.sendMessage("Aborting #" + eventId + " creation because its registration is very close to #" + activeTemplate.getName() + " 's registration area.");
				return false;
			}
		}
		try
		{	
			final AbstractEvent event = mkEvent(template);
			if (event != null)
			{
				event.startEvent();
//				if (Config.SERVER_LOCAL)
					for (final var player : L2World.getInstance().getAllPlayers().values())
						event.register(player);
				return true;
			}
			return false;
		}
		catch (Exception e)
		{	e.printStackTrace();
		}
		return false;
	}
	
	public static AbstractEvent mkEvent(final EventTemplate eventTemplate)
	{
		final Class<?> eventClass = eventTemplate.getWorldClass();
		try
		{
			return (AbstractEvent) eventClass.getConstructor(EventTemplate.class).newInstance(eventTemplate);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static <V> V jackpotPick(Collection<V> list, IJackpot<V> ijackpot)
	{
		if (list.isEmpty())
			return null;
		if (list.size() == 1)
			for (final V v : list)
				return v;
		int total = 0;
		for (final V v : list)
			total += ijackpot.getJackpotRate(v);
		final int roll = Rnd.get(0, total);
		int pool = 0;
		for (final V v : list)
		{
			pool += ijackpot.getJackpotRate(v);
			if (pool >= roll)
				return v;
		}
		return null;
	}
	
	public static <V> V jackpotPick(V[] list, IJackpot<V> ijackpot)
	{
		if (list.length == 0)
			return null;
		if (list.length == 1)
			for (final V v : list)
				return v;
		int total = 0;
		for (final V v : list)
			total += ijackpot.getJackpotRate(v);
		final int roll = Rnd.get(0, total);
		int pool = 0;
		for (final V v : list)
		{
			pool += ijackpot.getJackpotRate(v);
			if (pool >= roll)
				return v;
		}
		return null;
	}
	
	public static <V> V jackpotPick(List<V> list, IJackpot<V> ijackpot, V defaultValue, IConditional<V> cond)
	{	final int listSize = list.size();
		if (listSize > 1)
		{	if (cond == null)
			{	int total = 0;
				for (final V v : list)
					total += ijackpot.getJackpotRate(v);
				final int roll = Rnd.get(0, total);
				int pool = 0;
				for (final V v : list)
				{	pool += ijackpot.getJackpotRate(v);
					if (pool >= roll)
						return v;
				}	
			}
			else
				return jackpotPick(cond.validate(list), ijackpot, defaultValue, null);
		}
		else if (listSize == 1)
		{	final V value = list.get(0);
			if (cond == null)
				return value;
			return cond.validate(value);
		}
		else if (cond != null)
			return cond.validate(defaultValue);
		return defaultValue;
	}
	
	public static JackpotItem[] parseJackpotItems(final String str)
	{
		if (str == null)
			return null;
		final String[] jackpotItemsStr = str.replace(" ", "").split(",");
		final JackpotItem[] jackpotItems = new JackpotItem[jackpotItemsStr.length];
		for (int i = 0; i < jackpotItemsStr.length; i++)
			jackpotItems[i] = parseJackpotItem(jackpotItemsStr[i]);
		return jackpotItems;
	}
	
	public static JackpotItem[] parseJackpotItemsOrNull(final String str)
	{
		final JackpotItem[] jackpotItems = parseJackpotItems(str);
		
		return jackpotItems == null || jackpotItems.length == 0 ? null : jackpotItems;
	}
	
	public static JackpotItem parseJackpotItem(final String str)
	{
		final String[] jackpotItemStr = str.replace(" ", "").split(":");
		try
		{
			return new JackpotItem(Integer.parseInt(jackpotItemStr[0]), Integer.parseInt(jackpotItemStr[1]));
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse Jackpot item... Make sure it looks like X:N !");
			e.printStackTrace();
			return null;
		}
	}
	
	public static JackpotItemEx[] parseJackpotItemsEx(final String str)
	{		
		if (str == null)
			return null;
		final String[] jackpotItemsStr = str.replace(" ", "").split(",");
		final JackpotItemEx[] jackpotItems = new JackpotItemEx[jackpotItemsStr.length];
		for (int i = 0; i < jackpotItemsStr.length; i++)
			jackpotItems[i] = parseJackpotItemEx(jackpotItemsStr[i]);
		return jackpotItems;
	}
	
	public static JackpotItemEx[] parseJackpotItemsExOrNull(final String str)
	{
		final JackpotItemEx[] jackpotItems = parseJackpotItemsEx(str);
		
		return jackpotItems == null || jackpotItems.length == 0 ? null : jackpotItems;
	}	
	public static JackpotItemEx parseJackpotItemEx(final String str)
	{
		final String[] jackpotItemStr = str.replace(" ", "").split(":");
		

		
		try
		{
			final String[] jackpotItemStrEx = jackpotItemStr[0].split("-");
			
			int value0 = 0;
			int value1 = 0;
			
			if (jackpotItemStrEx.length == 1)
				value0 = value1 = Integer.parseInt(jackpotItemStr[0]);
			else
			{
				value0 = Integer.parseInt(jackpotItemStrEx[0]);
				value1 = Integer.parseInt(jackpotItemStrEx[1]);
			}
			
			return new JackpotItemEx(value0, value1, Integer.parseInt(jackpotItemStr[1]));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static class JackpotItem
	{
		private final int _value0;
		private final int _weight;
		
		public JackpotItem(final int value0, final int weight)
		{
			_value0 = value0;
			_weight = weight;
		}
		
		public int getValue()
		{
			return _value0;
		}
		
		public int getWeight()
		{
			return _weight;
		}
	}
	
	public static class JackpotItemEx extends JackpotItem
	{
		private final int _value1;
		
		public JackpotItemEx(final int value0, final int value1, final int weight)
		{
			super(value0, weight);
			_value1 = value1;
		}
		
		public int getValueEx()
		{
			return _value1;
		}
	}
	
	public interface IJackpot<V>
	{
		public int getJackpotRate(V v);
	}
	
	public static String formatPosition(int place)
	{
		switch (place)
		{	case 1:
				return "1st";
			case 2:
				return "2nd";
			case 3:
				return "3rd";
			default:
				return place + "th";
		}
	}
	
	public static AbstractWorld<?> newWorld(final WorldTemplate worldTemplate) throws Exception
	{
		final Class<?> worldClass = worldTemplate.getWorldClass();
		if (worldClass == null)
			return new AbstractWorld<WorldTemplate>(worldTemplate){};
		return (AbstractWorld<?>) worldClass.getConstructor(worldTemplate.getClass()).newInstance(worldTemplate);
	}
	
	public static long getTimePassed(final long t0, TimeUnit time)
	{
		final long t1 = System.currentTimeMillis();
		if (t0 > t1)
			return 0;
		return time.convert(t1 - t0, TimeUnit.MILLISECONDS);
	}
}
