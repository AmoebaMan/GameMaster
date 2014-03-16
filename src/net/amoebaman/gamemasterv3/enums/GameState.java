package net.amoebaman.gamemasterv3.enums;

/**
 * Represents all the various states the game master can be in, with regard to
 * games playing, etc.
 * 
 * @author AmoebaMan
 */
public enum GameState{
	
	/**
	 * The master is currently running a game, which may mess with players as it
	 * pleases
	 */
	RUNNING,
	
	/**
	 * The master is running a game, but operations have been temporarily
	 * suspended and games shouldn't be messing with players
	 */
	PAUSED,
	
	/**
	 * The master is currently in the intermission phase, and no games should be
	 * doing anything whatsoever
	 */
	INTERMISSION;
	
}
