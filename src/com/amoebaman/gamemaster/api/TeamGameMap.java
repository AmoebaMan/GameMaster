package com.amoebaman.gamemaster.api;

import java.io.File;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.amoebaman.gamemaster.api.TeamAutoGame.Team;
import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.S_Location;

public class TeamGameMap extends GameMap {
	
	public HashMap<Team, Location> respawns;
	
	public TeamGameMap(String name, World world){
		super(name, world);
		denullify();
	}
	
	public TeamGameMap(String name, ConfigurationSection yaml){
		super(name, yaml);
		if(yaml != null){
			ConfigurationSection section = yaml.getConfigurationSection("respawn");
			for(String key : section.getKeys(false))
				respawns.put(Team.getByString(key), S_Location.stringLoad(section.getString(key)));
		}
		denullify();
	}
	
	public TeamGameMap(File file){
		this(file.getName().substring(0, file.getName().indexOf('.')), YamlConfiguration.loadConfiguration(file));
		denullify();
	}
	
	public void save(ConfigurationSection config){
		super.save(config);
		for(Team color : respawns.keySet())
			config.set("respawn." + color.toString(), S_Location.stringSave(respawns.get(color)));
	}
	
	public void denullify(){
		super.denullify();
		if(respawns == null) respawns = new HashMap<Team, Location>();
	}
	
	public UniqueList<Team> getActiveTeams(){
		UniqueList<Team> list = new UniqueList<Team>();
		list.addAll(respawns.keySet());
		return list;
	}
	
	public int getNumTeams(){
		return respawns.size();
	}
	
}
