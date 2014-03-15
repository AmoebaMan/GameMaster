package net.amoebaman.gamemaster.enums;

/**
 * Represents the various states a player can be in with respect to the games.
 * 
 * @author AmoebaMan
 */
public enum PlayerStatus {
	
	/** The player is in the game, being managed and manipulated by it */
	PLAYING,
	
	/** The player is outside of the game, but certain actions are still
	 * prohibited by the GameMaster */
	EXTERIOR,
	
	/** The player is outside of the game and totally unbound by any rules
	 * set by the GameMaster */
	ADMIN,
	;
	
}
