package net.amoebaman.gamemaster.modules;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface RespawnModule{
	
	public int getRespawnSeconds(Player player);
	public Location getWaitingLoc(Player player);
	public Location getRespawnLoc(Player player);
	public int getRespawnInvulnSeconds(Player player);
	
}
