package net.amoebaman.gamemaster.utils;

import java.util.ArrayList;
import java.util.List;

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
		if(locs.contains(null))
			return null;
		return locs;
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
		else
			super.set(path, value);
	}
	
}
