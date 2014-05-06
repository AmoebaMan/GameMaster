package net.amoebaman.gamemasterv3.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;

import net.amoebaman.gamemasterv3.api.GameMap;
import net.amoebaman.amoebautils.S_Loc;

/**
 * An expansion of a {@link MemoryConfiguration} with additional helpful methods
 * for automatically deserializing various kinds of data commonly used by
 * {@link GameMap GameMaps}.
 * 
 * @author AmoebaMan
 */
public class PropertySet extends MemoryConfiguration{
	
	/**
	 * Gets the requested {@link World} by path, deserialized using
	 * {@link Bukkit#getWorld(String)}. Also see {@link #set(String, World)}.
	 * 
	 * @param path a config path
	 * @return the world, or null if none is found
	 */
	public World getWorld(String path){
		return Bukkit.getWorld(getString(path));
	}
	
	/**
	 * Gets the requested {@link Location} by path, deserialized using
	 * {@link S_Loc#stringLoad(String)}. Also see {@link #set(String, Location)}
	 * .
	 * 
	 * @param path a config path
	 * @return the location, or null if none was found
	 */
	public Location getLocation(String path){
		return S_Loc.stringLoad(getString(path));
	}
	
	/**
	 * Gets the requested list of {@link Location} by path, deserializing the
	 * individual items using {@link S_Loc#stringLoad(String)}. Also see
	 * {@link #set(String, Collection)}.
	 * 
	 * @param path
	 * @return
	 */
	public List<Location> getLocationList(String path){
		List<String> strs = getStringList(path);
		if(strs == null)
			return null;
		List<Location> locs = new ArrayList<Location>();
		for(String str : strs)
			locs.add(S_Loc.stringLoad(str));
		return locs.isEmpty() ? null : locs;
	}
	
	/**
	 * Sets the specified path to the given {@link World}, serialized using
	 * {@link World#getName()}. Also see {@link #getWorld(String)}.
	 * 
	 * @param path a config path
	 * @param value a world
	 */
	public void set(String path, World value){
		super.set(path, value.getName());
	}
	
	/**
	 * Sets the specified path to the given {@link Location}, serialized using
	 * {@link S_Loc#stringSave(Location, boolean, boolean)}. Also see
	 * {@link #getLocation(String)}.
	 * 
	 * @param path a config path
	 * @param value a location
	 */
	public void set(String path, Location value){
		super.set(path, S_Loc.stringSave(value, true, true));
	}
	
	/**
	 * Sets the specified path to the given collection of {@link Location},
	 * serialized into a list of strings using
	 * {@link S_Loc#stringSave(Location, boolean, boolean)}. Also see
	 * {@link #getLocationList(String)}.
	 * 
	 * @param path a config path
	 * @param locs a bunch of locations
	 */
	public void set(String path, Collection<Location> locs){
		if(locs == null)
			super.set(path, null);
		List<String> strs = new ArrayList<String>();
		for(Location loc : locs)
			strs.add(S_Loc.stringSave(loc, true, true));
		super.set(path, strs);
	}
	
}
