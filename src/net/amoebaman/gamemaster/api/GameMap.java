package net.amoebaman.gamemaster.api;

import org.bukkit.Bukkit;

import net.amoebaman.gamemaster.utils.PropertySet;

public class GameMap {
	
	/** The name of the map */
	public final String name;
	/** Contains all the values of the map in one place, to allow easy cross-compatibility */
	public final PropertySet properties;
	
	/**
	 * Creates a new ArenaMap with a specified name and no properties except those requried.
	 * Arena maps cannot contain spaces in their names.
	 * @param name the name of the map
	 * @throws IllegalArgumentException if a space is present in name
	 */
	public GameMap(String name) throws IllegalArgumentException{
		if(name.contains(" "))
			throw new IllegalArgumentException("map names cannot contain spaces");
		this.name = name;
		properties = new PropertySet();
		properties.options().pathSeparator('/');
		/*
		 * Add basic values that must always be present
		 */
		properties.set("world", Bukkit.getWorlds().get(0).getName());
	}
	
	public String toString(){ return name; }
	
	public boolean equals(Object x){
		if(x instanceof GameMap)
			return toString().equalsIgnoreCase(x.toString());
		else
			return false;
	}
	
	public int hashCode(){
		return toString().hashCode();
	}
	
}
