package net.amoebaman.gamemaster.enums;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * Represents the various teams that can be tracked by the GameMaster.
 * 
 * @author AmoebaMan
 */
public enum Team{
	
	/** The blue team */
	BLUE(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, true),
	
	/** The red team */
	RED(ChatColor.RED, DyeColor.RED, Material.RECORD_3, true),
	
	/** The green team */
	GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, Material.GREEN_RECORD, true),
	
	/** The yellow team */
	YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, Material.GOLD_RECORD, true),
	
	/** The black team */
	BLACK(ChatColor.BLACK, DyeColor.BLACK, Material.RECORD_8, true),
	
	/** The white team */
	WHITE(ChatColor.WHITE, DyeColor.WHITE, Material.RECORD_9, true),
	
	/** The purple team */
	PURPLE(ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA, Material.RECORD_12, true),
	
	/** The cyan team */
	CYAN(ChatColor.DARK_AQUA, DyeColor.CYAN, Material.RECORD_7, true),
	
	/** The special-case defending team, visually similar to the blue team */
	DEFEND(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, false),
	
	/** The special-case defending team, visually similar to the red team */
	ATTACK(ChatColor.RED, DyeColor.RED, Material.RECORD_3, false),
	
	/** The special-case team representing no team */
	NEUTRAL(ChatColor.GRAY, DyeColor.SILVER, Material.RECORD_11, false);
	;
	
	/** The chat color representing this team */
	public final ChatColor chat;
	
	/** The dye color representing this team */
	public final DyeColor dye;
	
	/** The music disc representing this team */
	public final Material disc;
	
	/** Whether or not this is a normal (as opposed to special-case) team */
	public final boolean normal;
	
	private Team(ChatColor chat, DyeColor dye, Material disc, boolean normal){
		this.chat = chat;
		this.dye = dye;
		this.disc = disc;
		this.normal = normal;
	}
	
	/**
	 * Gets the {@link org.bukkit.scoreboard.Team scoreboard team} representing
	 * this team, used to display scores on the Minecraft scoreboard. Players
	 * are automatically assigned to scoreboard teams to manage things like name
	 * color and friendly fire.
	 * 
	 * @return the scoreboard team
	 */
	public org.bukkit.scoreboard.Team getBukkitTeam(){
		if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(toString()) != null)
			return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(toString());
		else{
			org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(toString());
			team.setAllowFriendlyFire(false);
			team.setCanSeeFriendlyInvisibles(true);
			team.setDisplayName(chat + toString() + ChatColor.RESET);
			team.setPrefix(chat.toString());
			team.setSuffix(ChatColor.RESET.toString());
			return team;
		}
	}
	
	/**
	 * Unregisters this team's {@link org.bukkit.scoreboard.Team scoreboard
	 * team}.
	 */
	public void removeBukkitTeam(){
		getBukkitTeam().unregister();
	}
	
	/**
	 * Returns a nicely capitalized version of {@link #name()}.
	 */
	public String toString(){
		return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
	}
	
	/**
	 * Gets a team by its name.
	 * 
	 * @param str the name of a team
	 * @return the matching team, or null if none was found
	 */
	public static Team getByString(String str){
		str = ChatColor.stripColor(str);
		for(Team color : values())
			if(color.name().equalsIgnoreCase(str) && color != NEUTRAL)
				return color;
		return null;
	}
	
	/**
	 * Gets a team by its chat color.
	 * 
	 * @param chat the chat color of a team
	 * @return the matching team, or null if none was found
	 */
	public static Team getByChat(ChatColor chat){
		for(Team color : values())
			if(color.chat == chat)
				return color;
		return null;
	}
	
	/**
	 * Gets a team by its dye color.
	 * 
	 * @param dye the dye color of a team
	 * @return the matching team, or null if none was found
	 */
	public static Team getByDye(DyeColor dye){
		for(Team color : values())
			if(color.dye == dye)
				return color;
		return null;
	}
	
	/**
	 * Gets a team by its music disc.
	 * 
	 * @param disc the music disc of a team
	 * @return the matching team, or null if none was found
	 */
	public static Team getByDisc(Material disc){
		if(!disc.isRecord())
			return null;
		for(Team color : values())
			if(color.disc == disc)
				return color;
		return null;
	}
	
	/**
	 * Checks to see if this chat color represents a team.
	 * 
	 * @param chat a chat color
	 * @return true if this chat color represents a team, false otherwise
	 */
	public static boolean isTeam(ChatColor chat){
		return getByChat(chat) != null;
	}
	
	/**
	 * Checks to see if this dye color represents a team.
	 * 
	 * @param dye a dye color
	 * @return true if this dye color represents a team, false otherwise
	 */
	public static boolean isTeam(DyeColor dye){
		return getByDye(dye) != null;
	}

	/**
	 * Checks to see if this music disc represents a team.
	 * 
	 * @param disc a music disc
	 * @return true if this music disc represents a team, false otherwise
	 */
	public static boolean isTeam(Material disc){
		return getByDisc(disc) != null;
	}
}
