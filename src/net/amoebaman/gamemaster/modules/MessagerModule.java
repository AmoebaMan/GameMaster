package net.amoebaman.gamemaster.modules;

import java.util.List;

import org.bukkit.entity.Player;

/**
 * Implementing this module will allow games to take advantage of GameMaster's
 * built-in system for sending players messages at certain points during the
 * game.
 * 
 * @author AmoebaMan
 */
public interface MessagerModule{

	/**
	 * Gets a message that will automatically be sent to players when they join
	 * the game, such as when the game first starts or if they log in mid-game.
	 * 
	 * @param inContext the player who will receive the message
	 * @return messages to send
	 */
	public List<String> getWelcomeMessage(Player inContext);
	
	/**
	 * Gets a message that will automatically be sent to players when they
	 * respawn into the game, <b>if and only if the game also implements
	 * {@link RespawnModule}.</b>
	 * 
	 * @param inContext the player who will receive the message
	 * @return messages to send
	 */
	public List<String> getSpawnMessage(Player inContext);
	
}
