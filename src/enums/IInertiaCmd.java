package inertiax.enums;


import java.util.LinkedList;

public interface IInertiaCmd
{
	public enum EInertiaAct implements IInertiaCmd
	{
		ACT__REM_TARGET,

		ACT__SPREAD,
		ACT__MAYBE_SPREAD,
		
		ACT__TO_VILLAGE,
		
		ACT__WALK_PATH,
		
		;
	}
	
	public enum EInertiaEvt implements IInertiaCmd
	{
		EVT__START(EInertiaAct.ACT__MAYBE_SPREAD),
		EVT__END,

		EVT__ATK_TARGET,
		EVT__REM_TARGET(EInertiaAct.ACT__REM_TARGET),
		
		EVT__NEW_TARGET,

		EVT__NO_TARGET(EInertiaAct.ACT__WALK_PATH, EInertiaAct.ACT__SPREAD),
		EVT__ASSIST_NO_TARGET,

		EVT__CREDIT_END,
		
		EVT__ON_DEATH,
		EVT__ON_KILL(EInertiaAct.ACT__SPREAD),
		EVT__ON_ATTACK,
		EVT__ON_SKILLCAST,
		
		EVT__ON_NEW_TARGET,
		EVT__ON_REM_TARGET,
		
		
		EVT__FOLLOW_CLOSE,
		EVT__FOLLOW_FAR,
		
//		EVT__START_AUTO_ATTACK,
		
		EVT__FILT_SKILL,
		EVT__FILT_TARGET,
		
		EVT__WHILE_DEAD(EInertiaAct.ACT__TO_VILLAGE),
		EVT__WHILE_TARGET_DEAD,
		
		
		EVT__EXT_WALK_PATH,
		
		;
		
		private final LinkedList<IInertiaCmd> _cmdTriggers = new LinkedList<>();
		
		private EInertiaEvt()
		{
			_cmdTriggers.addFirst(this);
		}
		
		private EInertiaEvt(final EInertiaAct ... inertiaActs)
		{
			for (final EInertiaAct inertiaAct : inertiaActs)
				_cmdTriggers.add(inertiaAct);

			_cmdTriggers.addFirst(this);
		}
		
		public LinkedList<IInertiaCmd> getCmdTriggers()
		{
			return _cmdTriggers;
		}
		
	}
}
