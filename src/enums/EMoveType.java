package inertiax.enums;

public enum EMoveType
{
	Not_Set,
	Follow_Target,
	Current_Location,
	Saved_Location;
	
	@Override
	public String toString()
	{
		return super.toString().replace('_', ' ');
	}

}
