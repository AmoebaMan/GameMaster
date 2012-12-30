package com.amoebaman.gamemaster.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class S_Location{
	
	public static void configSave(Location loc, ConfigurationSection config){
		config.set("world", loc.getWorld().getName());
		config.set("x", loc.getX());
		config.set("y", loc.getY());
		config.set("z", loc.getZ());
		config.set("yaw", loc.getYaw());
		config.set("pitch", loc.getPitch());
	}
	
	public static Location configLoad(ConfigurationSection config){
		if(config == null)
			return new Location(Bukkit.getWorlds().get(0), 0.5, 64.5, 0.5);
		return new Location(Bukkit.getWorld(config.getString("world", "world")), config.getDouble("x", 0.5), config.getDouble("y", 64.5), config.getDouble("z", 0.5), (float)config.getDouble("yaw", 0.0), (float)config.getDouble("pitch", 0.0));
	}

	public static String stringSave(Location loc){
		return loc.getWorld().getName() + "@" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getYaw() + "," + loc.getPitch();
	}
	
	public static Location stringLoad(String str){
		try{
			World world = Bukkit.getWorld(str.substring(0, str.indexOf("@")));
			String[] coords = str.substring(str.indexOf("@") + 1).split(",");
			Location toReturn = new Location(world, Integer.parseInt(coords[0]) + 0.5, Integer.parseInt(coords[1]) + 0.5, Integer.parseInt(coords[2]) + 0.5);
			if(coords.length > 3){
				toReturn.setYaw(Float.parseFloat(coords[3]));
				toReturn.setPitch(Float.parseFloat(coords[4]));
			}
			return toReturn;	
		}
		catch(Exception e){
			Bukkit.getLogger().severe("Was unable to parse Location from String: " + str);
			return new Location(Bukkit.getWorlds().get(0), 0.5, 64.5, 0.5, 0, 0);
		}
	}
	
	public static String toString(Location loc){
		return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") in " + loc.getWorld().getName();
	}
}
