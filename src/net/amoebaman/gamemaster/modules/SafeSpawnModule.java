package net.amoebaman.gamemaster.modules;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SafeSpawnModule{
	
	public int getSpawnRadius(Player player);
	public int getSpawnReentryDelaySeconds(Player player);
	public Location getRespawnLoc(Player player);
	
}
