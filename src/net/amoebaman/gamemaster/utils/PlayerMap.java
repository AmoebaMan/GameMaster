package net.amoebaman.gamemaster.utils;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author AmoebaMan
 *
 * A class that attempts to overcome the traditional aversion to using org.bukkit.entity.Player as
 * the key type for a Map, namely that Player references can be quite large and we don't want to
 * keep them around after they're gone unless necessary.
 * <br><br>
 * This class is externally typed with {@link org.bukkit.entity.Player} as the key type, but internally
 * uses {@link java.lang.String} as the key type, using the player's name.
 * <br><br>
 * In addition to this memory-saving measure, this map also allows the contents to be accessed through
 * either the player's name or the player object itself, meaning no more hassle with {@link Player#getName()}
 * or {@link Bukkit#getPlayer(String)} when you want to pull out of a map.
 * <br><br>
 * If all this wasn't enough, you can also specify a default value when the map is initialized that will
 * be returned instead of null if the key you're looking for doesn't exist within the map.
 *
 * @param <V> whatever you want to store
 */
public class PlayerMap<V> extends HashMap<String, V>{

    private static final long serialVersionUID = 8042999281349275123L;
    private final V defaultValue;
	
	public PlayerMap(){ defaultValue = null; }
	
	public PlayerMap(V defaultV){ defaultValue = defaultV; }
	
	public V getDefaultValue(){
		return defaultValue;
	}

	public boolean containsKey(Object key) {
		if(key instanceof Player)
			return super.containsKey(((Player) key).getName());
		if(key instanceof String)
			return super.containsKey(key);
		return false;
	}

	public boolean containsValueType(V value){
		return super.containsKey(value);
	}
	
	public Set<Entry<Player, V>> playerEntrySet() {
		Set<Entry<Player, V>> toReturn = new HashSet<Entry<Player, V>>();
		for(String name : keySet())
			toReturn.add(new PlayerEntry(Bukkit.getPlayer(name), get(name)));
		return toReturn;
	}

	public V get(Object key) {
		V result = null;
		if(key instanceof Player)
			result = get(((Player) key).getName());
		if(key instanceof String)
			result = get(key);
		return (result == null) ? defaultValue : result;
	}
	
	public Set<Player> playerKeySet(){
		Set<Player> toReturn = new HashSet<Player>();
		for(String name : keySet())
			toReturn.add(Bukkit.getPlayer(name));
		return toReturn;
	}

	public V put(Player key, V value) {
		if(key == null)
			return null;
		return put(key.getName(), value);
	}

	public void putAllPlayers(Map<? extends Player, ? extends V> map) {
		for(Entry<? extends Player, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	public V remove(Object key) {
		if(key instanceof Player)
			return remove(((Player) key).getName());
		if(key instanceof String)
			return remove(key);
		return null;
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
