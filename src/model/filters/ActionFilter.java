package inertiax.model.filters;

import inertiax.model.Inertia;
import inertiax.model.InertiaCast;
import l2.ae.pvp.gameserver.model.actor.L2Character;

public class ActionFilter extends InertiaFilter<InertiaCast> 
{
	public ActionFilter(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public boolean test(final InertiaCast inertiaCast)
	{
		if (inertiaCast == null)
			return false;
		if (inertiaCast.isReuse())
			return false;
		final var player = _inertia.getActivePlayer();
		if (player == null)
			return false;
		if (!inertiaCast.isUserHp(player))
			return false;
		final L2Character targetPlayer = player.getTargetChar();
		if (targetPlayer != null && !inertiaCast.isTargetHp(targetPlayer))
			return false;
		return true;
	}
	
}