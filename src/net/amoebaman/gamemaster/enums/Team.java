package net.amoebaman.gamemaster.enums;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

public enum Team{
	
	BLUE(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, true),
	RED(ChatColor.RED, DyeColor.RED, Material.RECORD_3, true),
	GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, Material.GREEN_RECORD, true),
	YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, Material.GOLD_RECORD, true),
	BLACK(ChatColor.BLACK, DyeColor.BLACK, Material.RECORD_8, true),
	WHITE(ChatColor.WHITE, DyeColor.WHITE, Material.RECORD_9, true),
	PURPLE(ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA, Material.RECORD_12, true),
	CYAN(ChatColor.DARK_AQUA, DyeColor.CYAN, Material.RECORD_7, true),
	
	DEFEND(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, false),
	ATTACK(ChatColor.RED, DyeColor.RED, Material.RECORD_3, false),
	
	NEUTRAL(ChatColor.GRAY, DyeColor.SILVER, Material.RECORD_11, false);
	;
	
	public final ChatColor chat;
	public final DyeColor dye;
	public final Material disc;
	public final boolean normal;
	private Team(ChatColor chat, DyeColor dye, Material disc, boolean normal){
		this.chat = chat;
		this.dye = dye;
		this.disc = disc;
		this.normal = normal;
	}

	public org.bukkit.scoreboard.Team getBukkitTeam(){
		if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(toString()) != null)
			return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(toString());
		else{
			org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(toString());
			team.setAllowFriendlyFire(false);
			team.setCanSeeFriendlyInvisibles(true);
			team.setDisplayName(chat + toString() + ChatColor.RESET);
			team.setPrefix(chat.toString());
			return team;
		}
	}
	
	public void removeBukkitTeam(){
		getBukkitTeam().unregister();
	}
	
	public String toString(){
		return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
	}
	
	public static Team getByString(String str){
		str = ChatColor.stripColor(str);
		for(Team color : values())
			if(color.name().equalsIgnoreCase(str) && color != NEUTRAL)
				return color;
		return null;
	}
	
	public static Team getByChat(ChatColor chat){
		for(Team color : values())
			if(color.chat == chat)
				return color;
		return null;
	}
	
	public static Team getByDye(DyeColor dye){
		for(Team color : values())
			if(color.dye == dye)
				return color;
		return null;
	}
	
	public static Team getByDisc(Material disc){
		if(!disc.isRecord())
			return null;
		for(Team color : values())
			if(color.disc == disc)
				return color;
		return null;
	}
	
	public static boolean isTeam(ChatColor chat){
		return getByChat(chat) != null;
	}
	
	public static boolean isTeam(DyeColor dye){
		return getByDye(dye) != null;
	}
	
	public static boolean isTeam(Material disc){
		return getByDisc(disc) != null;
	}
}