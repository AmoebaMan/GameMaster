package com.amoebaman.gamemaster.utils;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.kitteh.tag.TagAPI;

import com.amoebaman.gamemaster.GameMaster;
import com.amoebaman.kitmaster.KitMaster;
import com.google.common.collect.Lists;

public class Utils {
	
	public static double distance(Location l1, Location l2){
		if(l1 == null || l2 == null)
			return Double.MAX_VALUE;
		return l1.distance(l2);
	}

	public static double distance(Entity e1, Entity e2){
		if(e1 == null || e2 == null)
			return Double.MAX_VALUE;
		return distance(e1.getLocation(), e2.getLocation());
	}
	
	public static double distance(Entity e, Location l){
		if(e == null)
			return Double.MAX_VALUE;
		return distance(e.getLocation(), l);
	}
	
	public static double spawnDistance(Player p){
		if(GameMaster.status.isActive)
			return distance(p, GameMaster.activeGame.getRespawnLoc(p));
		return distance(p, GameMaster.waitingRoom);
	}

	public static void clearInventory(Player player){
		player.closeInventory();
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		for(PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());
		for(LivingEntity entity : player.getWorld().getLivingEntities())
			if(entity instanceof Tameable){
				Tameable pet = (Tameable) entity;
				if(pet.getOwner() == null || pet.getOwner().equals(player))
					while(!entity.isDead())
						entity.damage(20);
			}
	}
	
	public static void resetPlayerStatus(Player player, boolean clearKits){
		clearInventory(player);
		TagAPI.refreshPlayer(player);
		player.eject();
		if(player.getVehicle() != null)
			player.getVehicle().eject();
		player.setHealth(20);
		player.setFoodLevel(20);
		player.setSaturation(5F);
		if(clearKits)
			KitMaster.clearAll(player);
	}
	
	public static void broadcast(List<String> messages){
		Bukkit.broadcastMessage(ChatUtils.spacerLine());
		for(String message : messages)
			Bukkit.broadcastMessage(ChatUtils.centerAlign(message));
		Bukkit.broadcastMessage(ChatUtils.spacerLine());
		GameMaster.lastBroadcast = System.currentTimeMillis();
	}
	
	public static void broadcast(String... messages){
		broadcast(Lists.newArrayList(messages));
	}
	
	public static void message(CommandSender recipient, List<String> messages){
		ChatUtils.sendSpacerLine(recipient);
		for(String message : messages)
			recipient.sendMessage(ChatUtils.centerAlign(message));
		ChatUtils.sendSpacerLine(recipient);
	}
	
	public static void message(CommandSender recipient, String... messages){
		message(recipient, Lists.newArrayList(messages));
	}

	public static void updatePlayerColors(){
		for(Player player : Bukkit.getOnlinePlayers()){
			String colorName = player.getName();
			if(GameMaster.players.get(player).isActive && GameMaster.status.isActive)
				colorName = GameMaster.activeGame.getChatColor(player) + colorName;
			player.setDisplayName(colorName);
			player.setPlayerListName(colorName.length() > 16 ? colorName.substring(0, 16) : colorName);
		}
	}
	
	public static boolean inTeamChat(Player player){
		if(!GameMaster.teamChat.containsKey(player))
			GameMaster.teamChat.put(player, false);
		return GameMaster.teamChat.get(player);
	}

	public static void toggleTeamChat(Player player){
		GameMaster.teamChat.put(player, !inTeamChat(player));
	}
	
	public static Player getKiller(Player victim){
		Player killer = victim.getKiller();
		if(killer == null && victim.getLastDamageCause() instanceof EntityDamageByEntityEvent){
			EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) victim.getLastDamageCause();
			if(damage.getDamager() instanceof Player)
				killer = (Player) damage.getDamager();
			if(damage.getDamager() instanceof Wolf && ((Wolf) damage.getDamager()).getOwner() instanceof Player)
				killer = (Player) ((Wolf) damage.getDamager()).getOwner();
			if(damage.getDamager() instanceof Projectile && ((Projectile) damage.getDamager()).getShooter() instanceof Player)
				killer = (Player) ((Projectile) damage.getDamager()).getShooter();
		}
		return killer;
	}

}
