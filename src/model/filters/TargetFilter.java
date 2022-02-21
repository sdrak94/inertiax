package inertiax.model.filters;

import inertiax.enums.EPanelOption;
import inertiax.model.Inertia;
import inertiax.model.panels.TargetFiltering;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2RaidBossInstance;

public class TargetFilter extends InertiaFilter<L2Character>
{
	public TargetFilter(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public boolean test(L2Character target)
	{
		final var activeChar = _inertia.getActivePlayer();
		
		if (target.isAlikeDead() || target.isFlatTarget())
			return false;
		
		if (target.getTarget() != activeChar && (target.getMaxHp() > 300_000 || target instanceof L2RaidBossInstance))
			return false;
		
		if (!target.isAutoAttackable(activeChar))
			return false;
		
		if (!activeChar.canSee(target))
			return false;
		
		final var panel = _inertia.getPanel(EPanelOption.Target_Filter);
		if (panel instanceof TargetFiltering targetFiltering && targetFiltering.isFilteredId(target.getNpcId()))
			return false;
		
		final var loc = _inertia.getSearchLocation();
		final var searchType = _inertia.getSearchType();
		if (!target.isInsideRadius(loc, searchType.getRange(), true, true))
			return false;
		
//		if (!GeoData.getInstance().canSeeTarget(target, loc))
//			return false;
		
		return true;
	}
}