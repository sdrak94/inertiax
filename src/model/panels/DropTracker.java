package inertiax.model.panels;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import inertiax.model.Inertia;
import inertiax.model.InertiaPanel;
import l2.ae.pvp.gameserver.cache.HtmCache;
import l2.ae.pvp.gameserver.datatables.IconsTable;
import l2.ae.pvp.gameserver.datatables.ItemTable;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;
import l2.ae.pvp.gameserver.templates.item.L2Item;
import l2.ae.pvp.gameserver.util.Util;

public class DropTracker extends InertiaPanel
{
	private static final int LIMIT = 6;
	
	private final Map<Integer, TrackedItem> _drops = new ConcurrentHashMap<>();
	
	private final AtomicLong _totalSum = new AtomicLong();
	
	private Instant cycle;
	
	public DropTracker(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public void render(final L2PcInstance viewer)
	{
		final String dropTemplate = HtmCache.getInstance().getHtm("data/html/aepvp/inertiax/template-drop.htm");
		
		final var player = _inertia.getActivePlayer();
		if (player == null)
			return;
		
		final var npcHtml = new NpcHtmlMessage();
		npcHtml.setFile("data/html/aepvp/inertiax/droptracker.htm");

		final ArrayList<TrackedItem> sortedItems = new ArrayList<>(_drops.values());
		Collections.sort(sortedItems);
		
		final StringBuilder sb = new StringBuilder(2048);
		
		int loops = 0;
		if (cycle != null) for (final var trackedItem : sortedItems)
		{
			final String perc = String.format("%.02f%%", trackedItem.getTotalPercent());
			
			sb.append(dropTemplate
					.replace("%ITEM_NAME%", trackedItem.getItemName())
					.replace("%ITEM_ICON%", trackedItem.getIcon())
					.replace("%COUNT%", String.valueOf(trackedItem.getCount()))
					.replace("%DPS%", trackedItem.getDPS(cycle))
					.replace("%PERC%", perc)
					);
			
			
			if (++loops > LIMIT)
				break;
		}
		
		npcHtml.replace("%drops%", sortedItems.size() > 0 ? sb.toString() : "<tr><td><font name=hs12 color=LEVEL>No drops recorded on this cycle</font></td></tr>");
		
		npcHtml.replace("%id%", _ownerId);
		
		player.sendPacket(npcHtml);
	}
	
	@Override
	protected boolean onBypass(final L2PcInstance actor, final String cmd, final StringTokenizer st)
	{
		if (cmd.startsWith("reset"))
		{
			reset();
			render(actor);
		}
		
		return false;
	
	}
	
	public void insertDrop(final int itemId, final long count)
	{
		if (cycle == null)
			cycle = Instant.now();
			
		_totalSum.addAndGet(count);
		final var trackedItem = _drops.computeIfAbsent(Integer.valueOf(itemId), r -> new TrackedItem(r));
		trackedItem.add(count);
	}

	public void reset()
	{
		cycle = null;
		_drops.clear();
		_totalSum.set(0L);
	}
	
	protected class TrackedItem implements Comparable<TrackedItem>
	{
		private final int _itemId;
		private final L2Item _item;
		private final String _icon;
		private final AtomicLong _count = new AtomicLong();
		
		private TrackedItem(final int itemId)
		{
			_itemId = itemId;
			_item = ItemTable.getInstance().getTemplate(itemId);
			_icon = IconsTable.getInstance().getItemIcon(itemId);
		}
		
		public String getItemName()
		{
			if (_item == null)
				return "?";
			return _item.getName();
		}
		
		public String getIcon()
		{
			return _icon;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public long getCount()
		{
			return _count.get();
		}
		
		public void add(final long delta)
		{
			_count.addAndGet(delta);
		}
		
		public double getTotalPercent()
		{
			return (_count.get() * 1d / _totalSum.get()) * 100;
		}
		
		public String getDPS(final Instant cycle)
		{
			final double count = _count.get();
			final var duration = Duration.between(cycle, Instant.now());

			final long seconds = duration.getSeconds();
			
			final double dps = count / seconds;
			if (dps > .99d)
				return Util.coolFormat(dps) + "/s";
			
			return String.format("%.02f/s", dps);
		}

		@Override
		public int compareTo(TrackedItem otherItem)
		{
			return Long.compare(otherItem.getCount(), getCount());
		}
	}
}
