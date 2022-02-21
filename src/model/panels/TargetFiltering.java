package inertiax.model.panels;

import java.util.StringTokenizer;

import gnu.trove.set.hash.TIntHashSet;
import inertiax.model.Inertia;
import inertiax.model.InertiaPanel;
import l2.ae.pvp.gameserver.datatables.NpcTable;
import l2.ae.pvp.gameserver.model.actor.L2Attackable;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;

public class TargetFiltering extends InertiaPanel
{


	private final TIntHashSet _filteredIds = new TIntHashSet();
	
	public TargetFiltering(Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public void render(final L2PcInstance viewer)
	{
		

	}

	
	@Override
	protected boolean onBypass(final L2PcInstance actor, final String cmd, final StringTokenizer st)
	{

	
	}
	
	public TIntHashSet getFilteredIds()
	{
		return _filteredIds;
	}
	
	public boolean isFilteredId(final int id)
	{
		return _filteredIds.contains(id);
	}
}
