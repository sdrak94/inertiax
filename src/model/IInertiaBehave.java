package inertiax.model;

import l2.ae.pvp.gameserver.model.L2Skill;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;

public interface IInertiaBehave
{
//	public void setAutoChill(final Inertia autoChill);
	
	public default void expand(IInertiaBehave behave)
	{
		
	}
	
	void onThinkStart();
	
	void onThinkEnd();
	
	void onDeath(L2Character killer);
	
	void onKill(L2Character victim);
	
	void onAttack(L2Character target);
	
	void onSkillCast(L2Skill skill);
	
	void onNewTarget(L2Character oldTarget, L2Character newTarget);
	
	boolean filterSkill(L2Skill skill);
	
	boolean filterTarget(L2Character target);
	
	void whileDead();
	
	void whileTargetDead();
	
	void onCreditsEnd();
	
	float lagMultiplier();
	
	void onUntarget();

	void onFollowClose(L2PcInstance assistPlayer);

	void onFollowFar(L2PcInstance assistPlayer);

	void onAssistNoTarget(L2PcInstance assistPlayer);

	void onStartAutoAttack(L2Character actualTarget);
	
	public void setInertia(final Inertia inertia);
	
	public Inertia getInertia();
	
	public default int getPriority()
	{
		return 0;
	}
	
	public L2Character searchTarget();
	
	public void onNoTargetFound();
}