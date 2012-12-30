package com.amoebaman.gamemaster.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class OfflinePlayerMap<V> extends PlayerMap<V>{

	@Override
	public void upkeep(){
		HashMap<String, V> newMap = new HashMap<String, V>();
		for(Entry<String, V> entry : contents.entrySet())
			if(entry.getKey() != null && entry.getValue() != null)
				newMap.put(entry.getKey(), entry.getValue());
		contents.clear();
		contents.putAll(newMap);
	}
	
	public Set<Entry<OfflinePlayer, V>> offlineEntrySet() {
		upkeep();
		Set<Entry<OfflinePlayer, V>> toReturn = new HashSet<Entry<OfflinePlayer, V>>();
		for(String name : contents.keySet())
			toReturn.add(new OfflinePlayerEntry(Bukkit.getOfflinePlayer(name), contents.get(name)));
		return toReturn;
	}
	
	public Set<OfflinePlayer> offlineKeySet(){
		upkeep();
		Set<OfflinePlayer> toReturn = new HashSet<OfflinePlayer>();
		for(String name : contents.keySet())
			toReturn.add(Bukkit.getOfflinePlayer(name));
		return toReturn;
	}
	
	public class OfflinePlayerEntry implements Map.Entry<OfflinePlayer, V>{

		private OfflinePlayer key;
		private V value;
		
		public OfflinePlayerEntry(OfflinePlayer key, V value){
			this.key = key;
			this.value = value;
		}
		
		public OfflinePlayer getKey() {
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
