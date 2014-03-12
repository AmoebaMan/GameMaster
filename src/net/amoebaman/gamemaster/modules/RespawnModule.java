package net.amoebaman.gamemaster.modules;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Implementing this module will allow games to take advantage of GameMaster's
 * built-in simple spawning system - when players die they are respawned to a
 * waiting location, and after a short wait they are sent to their respawn
 * point.
 * 
 * @author AmoebaMan
 */
public interface RespawnModule{

	/**
	 * Gets the time delay between when the player respawns, and when they are
	 * actually sent back to their respawn point in game, the amount of time
	 * they are confined to the waiting location.
	 * 
	 * @param player the player respawning
	 * @return the respawn delay in seconds
	 */
	public int getRespawnSeconds(Player player);
	
	/**
	 * Gets the location at which the player must wait out the respawn delay.
	 * They will be complete invulnerable while waiting at this location, so try
	 * to make sure they won't be able to actually get back onto the battlefield.
	 * 
	 * @param player the player respawning
	 * @return the waiting location
	 */
	public Location getWaitingLoc(Player player);
	
	/**
	 * Gets the respawn location that the player will be sent to once their
	 * repsawn delay has passed.  At this point the player will be once again
	 * completely back in the game.
	 * 
	 * @param player the player respawning
	 * @return the respawn location
	 */
	public Location getRespawnLoc(Player player);
	
	/**
	 * Gets the period after being sent back to their respawn point during which
	 * the player is fully invulnerable.  Use this for respawn safety if your
	 * implementation of {@link #getRespawnLoc(Player)} returns a different
	 * location than {@link SafeSpawnModule#getSafeLoc(Player)}.
	 * 
	 * @param player the player respawning
	 * @return the invulnerability time
	 */
	public int getRespawnInvulnSeconds(Player player);
	
}
