package net.amoebaman.gamemaster.modules;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Implementing this module will allow games to take advantage of GameMaster's
 * built-in spawn protection system, to prevent spawn-killing and other
 * spawn-related douchebaggery.
 * 
 * @author AmoebaMan
 */
public interface SafeSpawnModule{
	
	/**
	 * Gets the euclidean radial extent of the safe point, calculated using the
	 * distance formula (as opposed to taxicab geometry block distance).
	 * <br><br>
	 * 7 is usually a good generic number for an average-sized spawn.
	 * 
	 * @param player the player in question
	 * @return the spawn radius
	 */
	public int getSpawnRadius(Player player);
	
	/**
	 * Gets the re-entry timeout for the safe location.  Players attempting to
	 * re-enter the safe location will be booted out if they have been damaged
	 * by an enemy more recently than this delay, in order to prevent players
	 * from fleeing combat to their spawns.
	 * 
	 * @param player the player in question
	 * @return the spawn re-entry timeout in seconds
	 */
	public int getSpawnReentryDelaySeconds(Player player);
	
	/**
	 * Gets the safe location, typically the spawn point.  Unless you've got a
	 * special respawning case, this method generally ought to return the same
	 * location as {@link RespawnModule#getRespawnLoc(Player)}.
	 * 
	 * @param player the player in question
	 * @return the safe location
	 */
	public Location getSafeLoc(Player player);
	
}
