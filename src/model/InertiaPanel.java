package inertiax.model;

import java.util.StringTokenizer;

import inertiax.model.ext.InertiaExt;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;

public abstract class InertiaPanel extends InertiaExt
{
	protected final int _ownerId;
	
	protected final Inertia _inertia;
	
	public InertiaPanel(final Inertia inertia)
	{
		_inertia = inertia;
		_ownerId = inertia.getOwnerId();
		
		inertia.addInertiaExt(this);
	}

	public abstract void render(final L2PcInstance viewer);
	
	public final boolean onBypass(final L2PcInstance actor, final StringTokenizer st)
	{
		if (st.hasMoreTokens())
		{
			final String cmd = st.nextToken();
			
			if (cmd.startsWith("render"))
				render(actor);
			else
				return onBypass(actor, cmd, st);
		}
		
		return false;
	}
	
	protected boolean onBypass(final L2PcInstance actor, final String cmd, final StringTokenizer st)
	{
		return false;
	}
	
	public void onItemDrop(final int itemId, final long count) {};
	
	public void renderInertia(final L2PcInstance viewer)
	{
		_inertia.render(viewer);
	}
}
