package net.amoebaman.gamemaster.utils;

import java.util.*;
import java.util.Map.Entry;

import net.amoebaman.utils.S_Loc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;

public class PropertySet extends MemoryConfiguration{
	
	public World getWorld(String path){
		return Bukkit.getWorld(getString(path));
	}
	
	public Location getLocation(String path){
		return S_Loc.stringLoad(getString(path));
	}
	
	public List<Location> getLocationList(String path){
		List<String> strs = getStringList(path);
		if(strs == null)
			return null;
		List<Location> locs = new ArrayList<Location>();
		for(String str : strs)
			locs.add(S_Loc.stringLoad(str));
		if(locs.isEmpty())
			return null;
		return locs;
	}
	
	public Map<String,Location> getLocationMap(String path){
		List<String> strs = getStringList(path);
		if(strs == null)
			return null;
		Map<String,Location> map = new HashMap<String,Location>();
		for(String str : strs){
			String[] split = str.split(":");
			map.put(split[0], S_Loc.stringLoad(split[1]));
		}
		if(map.isEmpty())
			return null;
		return map;
	}
	
	public void set(String path, Object value){
		if(value instanceof World)
			super.set(path, ((World) value).getName());
		else if(value instanceof Location)
			super.set(path, S_Loc.stringSave((Location) value));
		else if(value instanceof List<?> && !((List<?>) value).isEmpty() && ((List<?>) value).get(0) instanceof Location){
			List<String> strs = new ArrayList<String>();
			List<Location> locs = (List<Location>) value;
			for(Location loc : locs)
				strs.add(S_Loc.stringSave(loc));
			super.set(path, strs);
		}
		else if(value instanceof Map<?,?> && !((Map<?,?>) value).isEmpty() && ((Map<?,?>) value).values().toArray()[0] instanceof Location){
			List<String> strs = new ArrayList<String>();
			Map<?,Location> map = (Map<?,Location>) value;
			for(Entry<?,Location> entry : map.entrySet())
				strs.add(entry.getKey() + ":" + S_Loc.stringSave(entry.getValue()));
			super.set(path, strs);
		}
		else
			super.set(path, value);
	}
	
}
