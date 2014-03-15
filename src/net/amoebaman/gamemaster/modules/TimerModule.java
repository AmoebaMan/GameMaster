package net.amoebaman.gamemaster.modules;

/**
 * Implementing this module will allow games to take advantage of GameMaster's
 * built-in game timer system, including a auto-run GUI timer at the top of the
 * screen.
 * 
 * @author AmoebaMan
 */
public interface TimerModule{
	
	/**
	 * Gets the maximum allowed length of the game.  After this time has run its
	 * course, the game will be ended using {@link #end()}.
	 * 
	 * @return the length of the game in minutes
	 */
	public int getGameLength();
	
	/**
	 * Ends the game once time has elapsed. This method should handle any and
	 * all map cleanup, announcements, and/or reward dispensation.
	 */
	public void end();
	
}
