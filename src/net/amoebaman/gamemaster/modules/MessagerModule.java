package net.amoebaman.gamemaster.modules;

import java.util.List;

import org.bukkit.entity.Player;

public interface MessagerModule{

	public List<String> getWelcomeMessage(Player inContext);
	public List<String> getSpawnMessage(Player inContext);
	
}
