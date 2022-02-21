package inertiax.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BiMap<K, VK, V> implements Map<K, Map<VK, V>>
{
	private final Map<K, Map<VK, V>> _mapImpl;
	private final Supplier<Map<VK, V>> _kmapFunc;
	
	public BiMap(Supplier<Map<K, Map<VK, V>>> func, Supplier<Map<VK, V>> kmapFunc) 
	{
		_mapImpl = func.get();
		_kmapFunc = kmapFunc;
	}

	public BiMap()
	{
		this(HashMap::new, HashMap::new);
	}
	
	public BiMap(final boolean concurrent)
	{
		this(concurrent ? ConcurrentHashMap::new : HashMap::new, HashMap::new);
	}
	
	public void put(K key1, VK key2, V value)
	{
		var map = _mapImpl.get(key1);
		if (map == null)
		{
			map = _kmapFunc.get();
			map.put(key2, value);
			_mapImpl.put(key1, map);
		}
		else
			map.put(key2, value);
	}
	
	public V get(K key1, VK key2)
	{
		final var map1 = _mapImpl.get(key1);
		if (map1 == null)
			return null;
		return map1.get(key2);
	}

	@Override
	public int size()
	{
		return _mapImpl.size();
	}

	@Override
	public boolean isEmpty()
	{
		return _mapImpl.isEmpty();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return _mapImpl.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return _mapImpl.containsValue(value);
	}

	@Override
	public Map<VK, V> get(Object key)
	{
		return _mapImpl.get(key);
	}

	@Override
	public Map<VK, V> put(K key, Map<VK, V> value)
	{
		return _mapImpl.put(key, value);
	}

	@Override
	public Map<VK, V> remove(Object key)
	{
		return _mapImpl.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends Map<VK, V>> m)
	{
		_mapImpl.putAll(m);
	}

	@Override
	public void clear()
	{
		_mapImpl.clear();
	}

	@Override
	public Set<K> keySet()
	{
		return _mapImpl.keySet();
	}

	@Override
	public Collection<Map<VK, V>> values()
	{
		return _mapImpl.values();
	}

	@Override
	public Set<Entry<K, Map<VK, V>>> entrySet()
	{
		return _mapImpl.entrySet();
	}
}
