package net.amoebaman.gamemaster.api;

import java.util.List;

import net.amoebaman.gamemaster.GameMaster;
import net.amoebaman.gamemaster.enums.MasterStatus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a game that can be run automatically using the GameMaster API.
 * 
 * @author AmoebaMan
 */
public abstract class AutoGame extends JavaPlugin{
	
	/**
	 * Gets the primary name of the game, for displaying purposes. This is drawn
	 * directly from the plugin name, replacing all underscores and dashes with
	 * spaces.
	 * 
	 * @return the name of the game
	 */
	public String getGameName(){
		return getDescription().getName().replace('-', ' ').replace('_', ' ');
	}
	
	/**
	 * We return {@link #getName()} so that auto games can be inserted directly
	 * into string literals for nice-looking results.
	 */
	public String toString(){
		return getGameName();
	}
	
	/**
	 * Checks to see if the game is active, that is, that it is playing
	 * actively. This should be checked prior to ALL event handling, so that
	 * events aren't handled in unexpected ways by inactive events.
	 * 
	 * @return true if the game is running, false otherwise
	 */
	public boolean isActive(){
		return GameMaster.status == MasterStatus.RUNNING && GameMaster.activeGame.equals(this);
	}
	
	/**
	 * Gets an array containing the aliases by which this game can be referred
	 * to. These are used as shortcut names for things like game voting.
	 * 
	 * @return the game's aliases
	 */
	public abstract String[] getAliases();
	
	/**
	 * Checks whether a {@link GameMap} is compatible with this game.
	 * GameMaps are designed to be universal and share all their data between
	 * games with each game using what it needs. This method should check to see
	 * that all the data the game needs is present in the properties of the map,
	 * and that the game can run safely.
	 * 
	 * @param map a map
	 * @return true if this game can run without error on the map, false
	 *         otherwise
	 */
	public abstract boolean isCompatible(GameMap map);
	
	/**
	 * Gets the color that should be appended to a player's name while this game
	 * is running. This is called periodically by the GameMaster to keep
	 * player's colors up to date. Returning null will cause all color to be
	 * removed from the player's name.
	 * 
	 * @param player a player
	 * @return the color the player's name should be
	 */
	public abstract ChatColor getNameColor(Player player);
	
	/**
	 * Gets a list of Strings to be sent to the player when they perform the
	 * /game command, requesting the status of the game. This should include
	 * score, time remaining, etc., all in a readable form.
	 * 
	 * @param player the player this information will be sent to
	 * @return the status of the game
	 */
	public abstract List<String> getStatus(Player player);
	
	/**
	 * Safely adds a player to the game, such as when they log in, or use the
	 * {@code /enter} command.
	 * 
	 * @param player a player
	 */
	public abstract void addPlayer(Player player);
	
	/**
	 * Safely removes a player from the game, such as when they log off, or use
	 * the {@code /exit} command.
	 * 
	 * @param player a player
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
