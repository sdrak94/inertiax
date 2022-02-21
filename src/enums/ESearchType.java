package inertiax.enums;


public enum ESearchType
{
	Off("FF6363"),
	Assist("LEVEL"),
	Close("63FF63"),
	Near("63FF63"),
	Far("63FF63");

	private final String _color;

	private ESearchType(final String color)
	{
		_color = color;
	}
	
	public String getColor()
	{
		return _color;
	}
	
	public int getRange()
	{
		switch (this)
		{
			case Off:
				return -1;
			case Assist:
				return 0;
			case Close:
				return 400;
			case Near:
				return 2000;
			case Far:
				return 3000;
		}
		return 0;
	}
}
