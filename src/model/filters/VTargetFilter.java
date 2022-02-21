package inertiax.model.filters;

import inertiax.model.Inertia;
import l2.ae.pvp.gameserver.model.actor.L2Character;

public class VTargetFilter extends InertiaFilter<L2Character>
{
	public VTargetFilter(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public boolean test(L2Character target)
	{
		if (target.isAlikeDead())
			return false;
		
		final var activeChar = _inertia.getActivePlayer();
		
		if (!activeChar.canSee(target))
			return false;
		
		if (!target.isAutoAttackable(activeChar))
			return false;
		return true;
	}
}