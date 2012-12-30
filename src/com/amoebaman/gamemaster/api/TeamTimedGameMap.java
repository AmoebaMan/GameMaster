package com.amoebaman.gamemaster.api;

import java.io.File;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class TeamTimedGameMap extends TeamGameMap {
	
	public int timeLimitMinutes;
	
	public TeamTimedGameMap(String name, World world){
		super(name, world);
		denullify();
	}
	
	public TeamTimedGameMap(String name, ConfigurationSection yaml){
		super(name, yaml);
		if(yaml != null)
			timeLimitMinutes = yaml.getInt("timeLimitMinutes");
		denullify();
	}
	
	public TeamTimedGameMap(File file){
		this(file.getName().substring(0, file.getName().indexOf('.')), YamlConfiguration.loadConfiguration(file));
		denullify();
	}
	
	public void save(ConfigurationSection config){
		super.save(config);
		config.set("timeLimitMinutes", timeLimitMinutes);
	}
	
	public void denullify(){
		super.denullify();
		if(timeLimitMinutes == 0) timeLimitMinutes = 15; //Default 15 minutes
	}
	
}
