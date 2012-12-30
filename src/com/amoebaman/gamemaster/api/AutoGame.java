package com.amoebaman.gamemaster.api;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.amoebaman.gamemaster.GameMaster;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.objects.UniqueList;

public abstract class AutoGame extends JavaPlugin{
	
	private String gameName;
	private String[] aliases;
	private int respawnSeconds;
	
	public void init(int respawnTime, String... aliases){
		gameName = getDescription().getName().replace('-', ' ');
		this.aliases = aliases;
		this.respawnSeconds = respawnTime;
		GameMaster.registerGame(this);
	}
	
	public final void onDisable(){
		if(isActive())
			endGame(null);
		GameMaster.deregisterGame(this);
	}
	
	public final String toString(){ return gameName.replaceAll("-", " "); }
	public final String getGameName(){ return toString(); }
	public final String[] getAliases(){ return aliases; }
	public final int getRespawnSeconds(){ return respawnSeconds; }

	public final boolean isActive(){
		if(GameMaster.status != MasterStatus.RUNNING)
			return false;
		return GameMaster.activeGame.equals(this);
	}
	
	public final String getMapsDirectory(){
		File dir = new File(GameMaster.getDirectory(this) + "/maps");
		dir.mkdirs();
		return dir.getPath();
	}
	
	//Methods for managing maps
	public abstract GameMap getActiveMap();
	public abstract GameMap getMap(String name);
	public abstract UniqueList<GameMap> getMaps();
	
	//Methods for mananging player participation
	public abstract ChatColor getChatColor(Player player);
	public abstract Location getRespawnLoc(Player player);
	public abstract void insertPlayer(Player toInsert);
	public abstract void extractPlayer(Player toExtract);
	public abstract List<String> getWelcomeMessage(Player inContext);
	public abstract List<String> getSpawnMessage(Player inContext);
	public abstract List<String> getStatus(Player inContext);
	
	//Methods for controlling game flow
	public abstract void prepGame();
	public abstract void startGame();
	public abstract void endGame(Object winner);
	
}
