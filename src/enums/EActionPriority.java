package inertiax.enums;

public enum EActionPriority
{

	Highest,
	Very_high,
	High,
	Medium,
	Low,
	Very_Low,
	Lowest,
	Remove;
	
	@Override
	public String toString()
	{
		return (ordinal() + 1) + " " + super.toString().replace('_', ' ');
	}
}
