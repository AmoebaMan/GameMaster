package net.amoebaman.gamemasterv3.api;

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
	
	
	
}
