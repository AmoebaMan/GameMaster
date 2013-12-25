package net.amoebaman.gamemaster.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerMap<V> implements Map<Player, V>{

	private final V defaultValue;
	private final Map<String, V> contents;
	
	public PlayerMap(){
		contents = new HashMap<String, V>();
		defaultValue = null;
	}
	
	public PlayerMap(V defaultV){
		contents = new HashMap<String, V>();
		defaultValue = defaultV;
	}
	
	public PlayerMap(Map<String, V> internal){
		contents = internal;
		defaultValue = null;
	}
	
	public PlayerMap(Map<String, V> internal, V defaultV){
		contents = internal;
		defaultValue = defaultV;
	}
	
	public V getDefaultValue(){
		return defaultValue;
	}
	
	public void clear() {
		contents.clear();
	}

	public boolean containsKey(Object key) {
		if(key instanceof Player)
			return contents.containsKey(((Player) key).getName());
		if(key instanceof String)
			return contents.containsKey(key);
		return false;
	}

	public boolean containsValue(Object value){
		return contents.containsValue(value);
	}
	
	public Set<Entry<Player, V>> entrySet() {
		Set<Entry<Player, V>> toReturn = new HashSet<Entry<Player, V>>();
		for(String name : contents.keySet())
			toReturn.add(new PlayerEntry(Bukkit.getPlayer(name), contents.get(name)));
		return toReturn;
	}

	public V get(Object key) {
		V result = null;
		if(key instanceof Player)
			result = contents.get(((Player) key).getName());
		if(key instanceof String)
			result = contents.get(key);
		return (result == null) ? defaultValue : result;
	}

	public boolean isEmpty(){
		return contents.isEmpty();
	}
	
	public Set<Player> keySet(){
		Set<Player> toReturn = new HashSet<Player>();
		for(String name : contents.keySet())
			toReturn.add(Bukkit.getPlayer(name));
		return toReturn;
	}

	public V put(Player key, V value) {
		if(key == null)
			return null;
		return contents.put(key.getName(), value);
	}
	
	public V put(String key, V value){
		return contents.put(key, value);
	}

	public void putAll(Map<? extends Player, ? extends V> map) {
		for(Entry<? extends Player, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	public V remove(Object key) {
		if(key instanceof Player)
			return contents.remove(((Player) key).getName());
		if(key instanceof String)
			return contents.remove(key);
		return null;
	}

	public int size() {
		return contents.size();
	}

	public Collection<V> values() {
		return contents.values();
	}
	
	public String toString(){
		return contents.toString();
	}
	
	public final Map<String, V> getInternalMap(){
		return contents;
	}
	
	public class PlayerEntry implements Map.Entry<Player, V>{

		private Player key;
		private V value;
		
		public PlayerEntry(Player key, V value){
			this.key = key;
			this.value = value;
		}
		
		public Player getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			V toReturn = this.value;
			this.value = value;
			return toReturn;
		}
		
	}
	
}
