package net.amoebaman.gamemaster.api;

import java.util.List;

import net.amoebaman.gamemaster.GameMaster;
import net.amoebaman.gamemaster.enums.MasterStatus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AutoGame extends JavaPlugin{
	
	/**
	 * Gets the primary name of the game, for displaying purposes.
	 * This is drawn directly from the plugin name, replacing all underscores and dashes with spaces.
	 * @return
	 */
	public String getGameName(){
		return getDescription().getName().replace('-', ' ').replace('_', ' ');
	}
	
	public String toString(){
		return getGameName();
	}
	
	/**
	 * Checks to see if the game is active, that is, that it is playing actively.
	 * This should be checked prior to ALL event handling, so that events aren't handled in unexpected ways by inactive events.
	 * @return
	 */
	public boolean isActive(){
		return GameMaster.status == MasterStatus.RUNNING && GameMaster.activeGame.equals(this);
	}
	
	/**
	 * Gets an array containing the aliases by which this game can be referred to.
	 * These are used as shortcut names for things like game voting.
	 * @return the aliases this game should respond to
	 */
	public abstract String[] getAliases();
	
	/**
	 * Checks whether a GameMap is compatible with this game.
	 * GameMaps are designed to be universal and share all their data between games with each game using what it needs.
	 * This method should check to see that all the data the game needs is present in the properties of the map, and that the game can run safely.
	 * @param map the map to check.
	 * @return true if this game can run without error on the map, false otherwise
	 */
	public abstract boolean isCompatible(GameMap map);
	
	/**
	 * Gets the color that should be appended to a player's name.
	 * This is called periodically by the GameMaster to keep player's colors up to date.
	 * Returning null will cause all color to be removed from the player's name.
	 * @param player the player
	 * @return the color the player's name should be
	 */
	public abstract ChatColor getNameColor(Player player);
	
	/**
	 * Gets a list of Strings to be sent to the player when they perform the /game command, requesting the status of the game.
	 * This should include score, time remaining, etc., all in a readable form.
	 * @param player the player this information will be sent to
	 * @return the status of the game
	 */
	public abstract List<String> getStatus(Player player);
	
	/**
	 * Safely adds a player to the game.
	 * @param player the player
	 */
	public abstract void addPlayer(Player player);
	
	/**
	 * Safely removes a player from the game.
	 * @param player the player
	 */
	public abstract void removePlayer(Player player);
	
	/**
	 * Starts the game.
	 */
	public abstract void start();
	
	/**
	 * Aborts the game.
	 */
	public abstract void abort();
	
}