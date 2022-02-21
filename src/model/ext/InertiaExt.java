package inertiax.model.ext;

import java.util.LinkedHashMap;
import java.util.Map;

import inertiax.enums.IInertiaCmd;
import inertiax.model.InertiaAct;

public class InertiaExt
{
	private Map<IInertiaCmd, IInertiaActImpl> _actImpls;
	
	public EActResult actExt(final IInertiaCmd einertiaCmd, final InertiaAct inertiaAct)
	{
		EActResult actResult = EActResult.ACT__CONTINUE;
		
		if (_actImpls != null)
		{		
			final IInertiaActImpl actImpl = _actImpls.get(einertiaCmd);
			if (actImpl != null)
				actResult = actImpl.act(inertiaAct);
		}

		return actResult;
	}
	
	{
		initExt();
	}
	
	protected void initExt()
	{
	}
	
	public void register(final IInertiaCmd einertiaCmd, final IInertiaActImpl actImpl)
	{
		if (_actImpls == null)
			_actImpls = new LinkedHashMap<>();
		
		_actImpls.put(einertiaCmd, actImpl);
	}
	
	public interface IInertiaActImpl
	{
		public EActResult act(final InertiaAct inertiaAct);
	}
	
	public enum EActResult
	{
		ACT__CONTINUE,
		ACT__EXIT,
		ACT__BREAK
		;
		
	}
}

