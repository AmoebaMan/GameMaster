package net.amoebaman.gamemaster.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class S_Loc{
	
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
		if(loc == null)
			return null;
		return loc.getWorld().getName() + "@" + (loc.getBlockX() + 0.5) + "," + (loc.getBlockY() + 0.5) + "," + (loc.getBlockZ() + 0.5) + "," + loc.getYaw() + "," + loc.getPitch();
	}
	
	public static Location stringLoad(String str){
		if(str == null)
			return null;
		try{
			String[] split = str.split("@");
			World world = Bukkit.getWorld(split[0]);
			String[] coords = split[1].split(",");
			Location toReturn = new Location(world, Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
			if(coords.length > 3){
				toReturn.setYaw(Float.parseFloat(coords[3]));
				toReturn.setPitch(Float.parseFloat(coords[4]));
			}
			return toReturn;	
		}
		catch(Exception e){
			Bukkit.getLogger().severe("Was unable to parse Location from String: " + str);
			return null;
		}
	}
	
	public static String toString(Location loc){
		return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") in " + loc.getWorld().getName();
	}
}
