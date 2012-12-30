package com.amoebaman.gamemaster.handlers;

import java.io.File;
import java.io.IOException;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

public class ChargeHandler {

	private static YamlConfiguration yamlConfig;
	
	public static void load(File file) throws IOException{
		yamlConfig = YamlConfiguration.loadConfiguration(file);
	}
	
	public static void save(File file) throws IOException{
		yamlConfig.save(file);
	}

	public static int getCharges(OfflinePlayer player){
		return yamlConfig.getInt(player.getName(), 0);
	}

	public static void setCharges(OfflinePlayer player, int amount){
		yamlConfig.set(player.getName(), amount);
	}

	public static void adjustCharges(OfflinePlayer player, int amount){
		setCharges(player, getCharges(player) + amount);
	}

}
