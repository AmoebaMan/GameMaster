package net.amoebaman.gamemasterv3.enums;

/**
 * Represents all the different states a player can be in with regard to the
 * GameMaster.
 * 
 * @author AmoebaMan
 */
public enum PlayerState{
	
	/**
	 * The player is actively playing in the game
	 */
	PLAYING,
	
	/**
	 * The player is passively observing the game
	 */
	WATCHING,
	
	/**
	 * The player is doing somethign completely unrelated to the game, and
	 * should under <b>no circumstances</b> be fucked with
	 */
	EXTERIOR;
	
}
