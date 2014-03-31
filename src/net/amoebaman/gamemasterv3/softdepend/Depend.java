package net.amoebaman.gamemasterv3.softdepend;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.amoebaman.statmaster.StatHandler;
import net.amoebaman.statmaster.StatMaster;

import net.milkbowl.vault.economy.Economy;

public class Depend{
	
	public static boolean hasKitMaster(){
		return Bukkit.getPluginManager().isPluginEnabled("KitMaster");
	}
	
	public static boolean hasVotifier(){
		return Bukkit.getPluginManager().isPluginEnabled("Votifier");
	}
	
	public static boolean hasHerochat(){
		return Bukkit.getPluginManager().isPluginEnabled("Herochat");
	}
	
	public static boolean hasStatMaster(){
		return Bukkit.getPluginManager().isPluginEnabled("StatMaster");
	}
	
	public static StatHandler getStats(){
		return StatMaster.getHandler();
	}
	
	public static Economy getEconomy(){
		if(!Bukkit.getPluginManager().isPluginEnabled("Vault"))
			return null;
		RegisteredServiceProvider<Economy> economy = Bukkit.getServicesManager().getRegistration(Economy.class);
        if(economy != null)
        	return economy.getProvider();
        else
        	return null;
	}
	
	public static boolean hasEconomy(){
		return getEconomy() != null;
	}
	
	public static boolean hasHolograms(){
		return Bukkit.getPluginManager().isPluginEnabled("HoloAPI");
	}
		
}
