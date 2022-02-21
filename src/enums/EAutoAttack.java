package inertiax.enums;

public enum EAutoAttack
{
	Never,
	Always,
	Skills_Reuse;
	
	@Override
	public String toString()
	{
		return super.toString().replace('_', ' ');
	}

}
