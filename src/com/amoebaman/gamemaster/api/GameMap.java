package com.amoebaman.gamemaster.api;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class GameMap{

	public static final Location origin = new Location(Bukkit.getWorld("world"), 0.5, 80.5, 0.5);
	
	public String name;
	public World world;
	public Weather weather;
	public Time time;
	
	public GameMap(){
		denullify();
	}
	
	public GameMap(String name, World world){
		this.name = name;
		this.world = world;
		denullify();
	}
	
	public GameMap(String name, ConfigurationSection yaml){
		this.name = name;
		if(yaml != null){
			world = Bukkit.getWorld(yaml.getString("world", "world"));
			weather = Weather.matchString(yaml.getString("weather", "random"));
			time = Time.matchString(yaml.getString("time", "random"));
		}
		denullify();
	}
	
	public GameMap(File file){
		this(file.getName().substring(0, file.getName().indexOf('.')), YamlConfiguration.loadConfiguration(file));
		denullify();
	}
	
	public void denullify(){
		if(name == null) name = "";
		if(world == null) world = Bukkit.getWorlds().get(0);
		if(weather == null) weather = Weather.RANDOM;
		if(time == null) time = Time.RANDOM;
	}
	
	public void save(ConfigurationSection config){
		config.set("world", world.getName());
		config.set("weather", weather.name());
		config.set("time", time.name());
	}
	
	public void save(File file) throws IOException{
		YamlConfiguration yaml = new YamlConfiguration();
		save(yaml);
		yaml.save(file);
	}
	
	public final boolean equals(Object other){
		if(other instanceof GameMap)
			return ((GameMap) other).name.equals(name);
		return false;
	}
	
	public final String toString(){
		return name;
	}
	
	public enum Time{
		DAWN(22000),
		DAY(6000),
		DUSK(14000),
		NIGHT(18000),
		RANDOM(0);
		public int ticks;
		private Time(int ticks){ this.ticks = ticks; }
		public static Time getRandom(){
			switch(new Random().nextInt(5)){
			case 0: return DAWN;
			case 1: return DUSK;
			case 2: return NIGHT;
			}
			return DAY;
		}
		public static Time matchString(String str){
			return valueOf(str.toUpperCase());
		}
	}
	
	public enum Weather{
		CLEAR,
		RAINING,
		STORMING,
		RANDOM;
		public static Weather getRandom(){
			switch(new Random().nextInt(5)){
			case 0: return RAINING;
			case 1: return STORMING;
			}
			return CLEAR;
		}
		public static Weather matchString(String str){
			return valueOf(str.toUpperCase());
		}
	}
	
}
