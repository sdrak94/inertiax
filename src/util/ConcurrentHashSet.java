package inertiax.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<E> implements Set<E>
{
	protected final Set<E> _values;

	public ConcurrentHashSet()
	{
		_values = Collections.newSetFromMap(new ConcurrentHashMap<E, Boolean>());
	}
	
	public ConcurrentHashSet(int size)
	{
		_values = Collections.newSetFromMap(new ConcurrentHashMap<E, Boolean>(size));
	}
	
	@Override
	public boolean add(E e)
	{
		return _values.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends E> collection)
	{
		return _values.addAll(collection);
	}

	@Override
	public void clear()
	{
		_values.clear();
	}

	@Override
	public boolean contains(Object e)
	{
		return _values.contains(e);
	}

	@Override
	public boolean containsAll(Collection<?> collection)
	{
		return _values.containsAll(collection);
	}

	@Override
	public boolean isEmpty()
	{
		return _values.isEmpty();
	}

	@Override
	public Iterator<E> iterator() 
	{
		return _values.iterator();
	}

	@Override
	public boolean remove(Object e)
	{
		return _values.remove(e);
	}

	@Override
	public boolean removeAll(Collection<?> collection)
	{
		return _values.removeAll(collection);
	}

	@Override
	public boolean retainAll(Collection<?> collection) 
	{
		return _values.retainAll(collection);
	}

	@Override
	public int size()
	{
		return _values.size();
	}

	@Override
	public Object[] toArray()
	{
		return _values.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arr)
	{
		return _values.toArray(arr);
	}

}
