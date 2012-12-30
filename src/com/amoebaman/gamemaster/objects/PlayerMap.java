package com.amoebaman.gamemaster.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Offers a safer and less memory-expensive alternative to storing values in a Map<Player, V>, and a more convenient alternative to using a Map<String, V> with the player's names.
 * 
 * This class is a wrapper for a HashMap<String, >.  When storing and retrieving values, it uses Player.getName() as the key.
 * 
 * This map guarantees not to keep records of offline players.  If you need to keep records for players that may not be online, use an OfflinePlayerMap.
 * 
 * This map offers the option to be given a default value.  When a default value is specified via constructor, the get method will guarantee a non-null return by replacing a null return with the default value.
 * 
 * @author Dennison
 * @param <V> The type that will be mapped to the Players
 */

public class PlayerMap<V> implements Map<Player, V>{

	protected final V defaultValue;
	protected final Map<String, V> contents;
	
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
	
	public void upkeep(){
		HashMap<String, V> newMap = new HashMap<String, V>();
		for(Entry<String, V> entry : contents.entrySet())
			if(Bukkit.getPlayer(entry.getKey()) != null && entry.getValue() != null)
				newMap.put(entry.getKey(), entry.getValue());
		contents.clear();
		contents.putAll(newMap);
	}
	
	public V getDefaultValue(){
		return defaultValue;
	}
	
	public void clear() {
		contents.clear();
	}

	public boolean containsKey(Object key) {
		upkeep();
		if(key instanceof Player)
			return contents.containsKey(((Player) key).getName());
		if(key instanceof String)
			return contents.containsKey(key);
		return false;
	}

	public boolean containsValue(Object value){
		upkeep();
		return contents.containsValue(value);
	}
	
	public Set<Entry<Player, V>> entrySet() {
		upkeep();
		Set<Entry<Player, V>> toReturn = new HashSet<Entry<Player, V>>();
		for(String name : contents.keySet())
			toReturn.add(new PlayerEntry(Bukkit.getPlayer(name), contents.get(name)));
		return toReturn;
	}

	public V get(Object key) {
		upkeep();
		V result = null;
		if(key instanceof Player)
			result = contents.get(((Player) key).getName());
		if(key instanceof String)
			result = contents.get(key);
		return (result == null) ? defaultValue : result;
	}

	public boolean isEmpty(){
		upkeep();
		return contents.isEmpty();
	}
	
	public Set<Player> keySet(){
		upkeep();
		Set<Player> toReturn = new HashSet<Player>();
		for(String name : contents.keySet())
			toReturn.add(Bukkit.getPlayer(name));
		return toReturn;
	}

	public V put(Player key, V value) {
		upkeep();
		if(key == null)
			return null;
		return contents.put(key.getName(), value);
	}
	
	public V put(String key, V value){
		upkeep();
		return contents.put(key, value);
	}

	public void putAll(Map<? extends Player, ? extends V> map) {
		upkeep();
		for(Entry<? extends Player, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	public V remove(Object key) {
		upkeep();
		if(key instanceof Player)
			return contents.remove(((Player) key).getName());
		if(key instanceof String)
			return contents.remove(key);
		return null;
	}

	public int size() {
		upkeep();
		return contents.size();
	}

	public Collection<V> values() {
		upkeep();
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
