package inertiax.model.ext;

import inertiax.enums.IInertiaCmd.EInertiaEvt;
import inertiax.model.Inertia;
import inertiax.model.InertiaAct;
import l2.ae.pvp.gameserver.ai.CtrlIntention;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.serverpackets.MyTargetSelected;

public class PlayerExt extends InertiaExt
{
	@Override
	protected void initExt()
	{
		register(EInertiaEvt.EVT__ATK_TARGET, this::atkTarget);
		register(EInertiaEvt.EVT__REM_TARGET, this::remTarget);
		register(EInertiaEvt.EVT__NEW_TARGET, this::newTarget);
		

		register(EInertiaEvt.EVT__CREDIT_END, this::creditsEnd);

		register(EInertiaEvt.EVT__WHILE_TARGET_DEAD, this::whileTargetDead);
		register(EInertiaEvt.EVT__WHILE_DEAD, this::whileDead);

		register(EInertiaEvt.EVT__FOLLOW_CLOSE, this::onFollowClose);
		register(EInertiaEvt.EVT__FOLLOW_FAR, this::onFollowFar);

		register(EInertiaEvt.EVT__ASSIST_NO_TARGET, this::onAssistNoTarget);

		super.initExt();
	}
	
	private EActResult atkTarget(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		final L2Character actualTarget = inertiaAct.getTarget();
		
		if (player != null && player != actualTarget && actualTarget.isAutoAttackable(player))
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
		
		return EActResult.ACT__CONTINUE;
	}
	
	private EActResult remTarget(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		if (player.isCastingNow())
			player.breakCast();
		if (player.isAttackingNow())
			player.breakAttack();
		player.setTarget(null);

		return EActResult.ACT__CONTINUE;
	}
	
	private EActResult newTarget(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		final L2Character newTarget = inertiaAct.getTarget();
		
		player.setTarget(newTarget);
		player.sendPacket(new MyTargetSelected(player, newTarget));
		
		return EActResult.ACT__CONTINUE;
	}

	private EActResult whileTargetDead(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		player.setTarget(null);
		
		return EActResult.ACT__CONTINUE;
	}
	
	private EActResult whileDead(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		final Inertia inertia = inertiaAct.getInertia();
		
		if (player.isRealPlayer())
			inertia.setRunning(false);
		
		return EActResult.ACT__CONTINUE;
	}
	
	private EActResult creditsEnd(final InertiaAct inertiaAct)
	{
		final Inertia inertia = inertiaAct.getInertia();
		inertia.setRunning(false);
		inertia.render();
		
		return EActResult.ACT__EXIT;
	}
	
	private EActResult onFollowClose(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);	

		return EActResult.ACT__EXIT;
	}
	
	private EActResult onFollowFar(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		final L2PcInstance assistPlayer = inertiaAct.getAssistPlayer();
		
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, assistPlayer);

		return EActResult.ACT__EXIT;
	}
	
	private EActResult onAssistNoTarget(final InertiaAct inertiaAct)
	{
		final L2PcInstance player = inertiaAct.getActivePlayer();
		
		final L2PcInstance assistPlayer = inertiaAct.getAssistPlayer();
		
		player.setTarget(assistPlayer);
		player.sendPacket(new MyTargetSelected(player, assistPlayer));

		return EActResult.ACT__EXIT;
	}

}
