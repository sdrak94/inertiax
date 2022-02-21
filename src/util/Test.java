package inertiax.util;

import java.util.HashSet;
import java.util.Iterator;

public class Test
{
	public static void main(String[] args) throws Exception
	{
	//	final IWorld world = WorldController.getInstance().getOrMake("Test");
		
		//final long t1 = System.currentTimeMillis();
		
	}
	
	public static class Wrapper
	{
		private final String _str;
		
		public Wrapper(String str)
		{
			_str = str;
		}
		
		public String getStr()
		{
			return _str;
		}
	}
	
	public static class WrapperHolder implements Iterable<String>
	{
		private final HashSet<Wrapper> _wrappers = new HashSet<>();
		
		public void add(Wrapper wrap)
		{
			_wrappers.add(wrap);
		}

		@Override
		public Iterator<String> iterator()
		{
			return new Iterator<String>()
			{
				private final Iterator<Wrapper> _passportIterator = _wrappers.iterator();
				
				@Override
				public boolean hasNext()
				{
					return _passportIterator.hasNext();
				}

				@Override
				public String next()
				{
					final Wrapper wrapper = _passportIterator.next();
					final String str = wrapper.getStr();
					return str == null && hasNext() ? next() : str;
				}
			};
		}
	}
}
