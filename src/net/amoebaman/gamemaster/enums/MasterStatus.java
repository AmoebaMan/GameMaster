package net.amoebaman.gamemaster.enums;

/**
 * Represents the various states the GameMaster can be in.
 * 
 * @author AmoebaMan
 */
public enum MasterStatus {

	/** The GameMaster is running a game */
	RUNNING(true),
	
	/** The GameMaster is running a game, but operations are suspended */
	SUSPENDED(true),
	
	/** The GameMaster is in intermission phase - no game is running, and
	 * the next game is being selected */
	INTERMISSION(false),
	
	/** The GameMaster is preparing the run the next game - no game is
	 * running, but the next one is chosen and the next map is being
	 * selected */
	PREP(false),
	;
	
	/** Whether or not the state is considered "active", specifically
	 * whether or not individual game plugins should be doing things
	 * during this state */
	public boolean active;
	
	private MasterStatus(boolean active){ this.active = active; }
	
}
