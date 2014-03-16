package net.amoebaman.gamemasterv3.api;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Represents a game that the game master can run automatically.
 * 
 * @author AmoebaMan
 */
public abstract class AutoGame{
	
	/**
	 * Gets the name of the game, in a nice, presentable form.
	 * 
	 * @return the name
	 */
	public abstract String getName();
	
	/**
	 * Gets a set of aliases that this game can also be referrenced by. These
	 * may be contractions, abbreviations, or whatever. They will never be
	 * directly displayed.
	 * 
	 * @return any aliases
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
	public abstract ChatColor getColor(Player player);
	
	/**
	 * Safely adds a player to the game, such as when they log in, or use the
	 * {@code /enter} command.
	 * 
	 * @param player a player
	 */
	public abstract void join(Player player);
	
	/**
	 * Safely removes a player from the game, such as when they log off, or use
	 * the {@code /exit} command.
	 * 
	 * @param player a player
	 */
	public abstract void leave(Player player);
	
	/**
	 * Starts the game.
	 */
	public abstract void start();
	
	/**
	 * Aborts the game.
	 */
	public abstract void abort();
	
}
