package inertiax.model;

import l2.ae.pvp.gameserver.datatables.ItemTable;
import l2.ae.pvp.gameserver.datatables.SkillTable;
import l2.ae.pvp.gameserver.model.L2Skill;
import l2.ae.pvp.gameserver.model.actor.L2Character;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;

public class InertiaCast
{
	private final int		_actionId;
	private final boolean	_isSkill;
	private double			_userHp	= 100;
	private double			_targHp	= 100;
	private long			_reuse;
	private long			_lastUse;
	
	public InertiaCast(final int actionId, final boolean isSkill)
	{
		_actionId = actionId;
		_isSkill = isSkill;
	}
	
	public int getActionId()
	{
		return _actionId;
	}
	
	public String getIcon()
	{
		return _isSkill ? SkillTable.getInstance().getSkill(_actionId, 1).getIcon() : ItemTable.getInstance().getTemplate(_actionId).getIcon();
	}
	
	public boolean isReuse()
	{
		return _lastUse + _reuse > System.currentTimeMillis();
	}
	
	public void initReuse()
	{
		_lastUse = System.currentTimeMillis();
	}
	
	public double getReuse()
	{
		return _reuse / 1000d;
	}
	
	public void setReuse(final double reuseSec)
	{
		_reuse = Math.min((long) (reuseSec * 1000L), 300_000);
	}
	
	public void setUserHP(final double userHp)
	{
		_userHp = Math.min(100d, userHp);
	}
	
	public boolean isUserHp(final L2PcInstance player)
	{
		return player.getHpPercent() <= _userHp;
	}
	
	public double getUserHp()
	{
		return _userHp;
	}
	
	public void setTargetHP(final double targHp)
	{
		_targHp = Math.min(100d, targHp);
	}
	
	public boolean isTargetHp(final L2Character target)
	{
		return target.getHpPercent() <= _targHp;
	}
	
	public double getTargetHp()
	{
		return _targHp;
	}
	
	public boolean isSkill()
	{
		return _isSkill;
	}
	
	public boolean isUsableNow(final L2PcInstance player)
	{
		if (isReuse())
			return false;
		
		if (isSkill())
		{
			final int level = player.getSkillLevel(_actionId);
			if (level > 0)
			{
				final L2Skill skill = SkillTable.getInstance().getInfo(_actionId, level);
				
				if (skill != null && !player.testDoCastConditions(skill))
				{
					return false;
				}
			}

		}
		
		return true;
	}
}