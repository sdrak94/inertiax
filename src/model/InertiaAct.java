package inertiax.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

import inertiax.enums.IInertiaCmd;
import inertiax.enums.IInertiaCmd.EInertiaEvt;
import inertiax.model.ext.InertiaExt;
import inertiax.model.ext.InertiaExt.EActResult;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;

public class InertiaAct
{
	private HashMap<IInertiaCmd, AtomicLong> _evtLags;
	
	private LinkedList<InertiaExt> _inertiaExts;
	
	private L2Character _target = null;
	private L2PcInstance _assistPlayer = null;
	private int _lag;
	
	private boolean _exit;
	
	protected final Inertia _inertia;
	
	public InertiaAct(final Inertia inertia)
	{
		_inertia = inertia;
	}
	
	public void addCmdLag(final IInertiaCmd inertiaCmd, final long addLag)
	{
		if (_evtLags == null)
			_evtLags = new HashMap<>();
		
		final AtomicLong lag = _evtLags.computeIfAbsent(inertiaCmd, eact -> new AtomicLong());
		
		lag.addAndGet(addLag);
	}
	
	public boolean hasCmdLag(final IInertiaCmd inertiaCmd)
	{
		if (_evtLags == null)
			return false;
		return _evtLags.containsKey(inertiaCmd);
	}
	
	public void tick(final long tick)
	{
		if (_evtLags != null)
			_evtLags.values().removeIf((al) -> al.addAndGet(-tick) < 1);
	}
	
	protected void reset()
	{
		_target = null;
		_assistPlayer = null;
		_lag = 0;
		_exit = false;
	}
	
	public void setTarget(final L2Character target)
	{
		_target = target;
	}
	
	public L2Character getTarget()
	{
		return _target;
	}
	
	public void setAssistPlayer(final L2PcInstance assistPlayer)
	{
		_assistPlayer = assistPlayer;
	}
	
	public L2PcInstance getAssistPlayer()
	{
		return _assistPlayer;
	}
	
	public int getInertiaLag()
	{
		return _lag;
	}
	
	public void addInertiaLag(final int lag)
	{
		_lag += lag;
	}
	
	private void exit()
	{
		_exit = true;
	}
	
	public void addInertiaExt(final InertiaExt inertiaExt)
	{
		if (_inertiaExts == null)
			_inertiaExts = new LinkedList<>();
		_inertiaExts.add(inertiaExt);
	}
	
	protected void evt(final EInertiaEvt inertiaEvt)
	{
		preprocessEvt(inertiaEvt);
		
		if (_exit)
			return;
		
		if (_inertiaExts != null && !hasCmdLag(inertiaEvt))
		{
			for (final InertiaExt inertiaExt : _inertiaExts)
			{
				final LinkedList<IInertiaCmd> triggerActs = inertiaEvt.getCmdTriggers();
				
				for (final IInertiaCmd inertiaCmd : triggerActs)
				{
					if (!hasCmdLag(inertiaCmd))
					{
						final EActResult actResult = inertiaExt.actExt(inertiaCmd, InertiaAct.this);
						
						switch (actResult)
						{
							case ACT__EXIT:
								exit();
							case ACT__BREAK:
								return;
						}
					}
				}
			}
		}
	}
	
	private void preprocessEvt(final EInertiaEvt inertiaEvt)
	{
		switch (inertiaEvt)
		{
			case EVT__START:
				onStart();
				return;
			case EVT__END:
				onEnd();
				return;
		}
	}
	
	public Inertia getInertia()
	{
		return _inertia;
	}
	
	public L2PcInstance getActivePlayer()
	{
		return _inertia.getActivePlayer();
	}
	
	private void onStart()
	{
		
	}
	
	private void onEnd()
	{
		_inertia.addLag(_lag);
		reset();
	}
	
}
