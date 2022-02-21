package inertiax.model.filters;

import inertiax.model.Inertia;
import inertiax.model.InertiaCast;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;

public class AvailSkillActionFilter extends InertiaFilter<InertiaCast> 
{
	public AvailSkillActionFilter(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public boolean test(final InertiaCast inertiaCast)
	{
		if (inertiaCast == null)
			return false;
		
		final L2PcInstance player = _inertia.getActivePlayer();
		
		if (player == null)
			return false;
		
		final var skill = _inertia.getSkill(inertiaCast);
		
		
		if (skill == null)
			return false;
		
		if (player.isSkillDisabled(skill))
			return false;
		
		if (!player.testDoCastConditions(skill))
			return false;
		
		if (!skill.testCondition(player, player.getTarget(), false))
			return false;
		
		return true;
	}
	
}