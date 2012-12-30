package com.amoebaman.gamemaster.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.S_Location;

public class FFAGameMap extends GameMap {
	
	public UniqueList<Location> respawns;
	
	public FFAGameMap(String name, World world){
		super(name, world);
		denullify();
	}
	
	public FFAGameMap(String name, ConfigurationSection yaml){
		super(name, yaml);
		if(yaml != null){
			for(String loc : yaml.getStringList("respawns"))
				respawns.add(S_Location.stringLoad(loc));
		}
		denullify();
	}
	
	public FFAGameMap(File file){
		this(file.getName().substring(0, file.getName().indexOf('.')), YamlConfiguration.loadConfiguration(file));
		denullify();
	}
	
	public void save(ConfigurationSection config){
		super.save(config);
		List<String> stringLocs = new ArrayList<String>();
		for(Location loc : respawns)
			stringLocs.add(S_Location.stringSave(loc));
		config.set("respawns", stringLocs);
	}
	
	public void denullify(){
		super.denullify();
		if(respawns == null) respawns = new UniqueList<Location>();
	}
	
	public UniqueList<Location> getFreeSpawns(){
		return respawns;
	}

}
