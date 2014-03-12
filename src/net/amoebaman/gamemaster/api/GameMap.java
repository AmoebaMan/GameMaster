package net.amoebaman.gamemaster.api;

import org.bukkit.Bukkit;

import net.amoebaman.gamemaster.utils.PropertySet;

/**
 * Represents a map for {@link AutoGame}{@code s} to run on. Maps are designed
 * to be universal - all their data is stored in a properties map, and each game
 * should draw on whatever it requires to run. Games check for map compatibility
 * with {@link AutoGame#isCompatible(GameMap)}.
 * 
 * @author AmoebaMan
 */
public class GameMap{
	
	/** The name of the map */
	public final String name;
	
	/**
	 * Contains all the values of the map in one place, to allow easy
	 * cross-compatibility
	 */
	public final PropertySet properties;
	
	/**
	 * Creates a new ArenaMap with a specified name and no properties except
	 * those requried by default. Game maps cannot contain spaces in their
	 * names, so all spaces will be removed from the name.
	 * 
	 * @param name the name of the map
	 */
	public GameMap(String name) throws IllegalArgumentException{
		this.name = name.replace(" ", "");
		properties = new PropertySet();
		properties.options().pathSeparator('/');
		/*
		 * Add basic values that must always be present
		 */
		properties.set("world", Bukkit.getWorlds().get(0).getName());
	}
	
	/**
	 * We return {@link #name} so that game maps can be inserted directly
	 * into string literals for nice-looking results.
	 */
	public String toString(){
		return name;
	}
	
	/**
	 * Maps check for equality based on their name, to avoid maps with duplicate
	 * names.
	 */
	public boolean equals(Object x){
		if(x instanceof GameMap)
			return toString().equalsIgnoreCase(x.toString());
		else
			return false;
	}
	
	/**
	 * Maps hash themselves based on their name, to avoid maps with duplicate
	 * names.
	 */
	public int hashCode(){
		return toString().hashCode();
	}
	
}
