package net.amoebaman.gamemaster;

import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.modules.SafeSpawnModule;
import net.amoebaman.gamemaster.modules.TimerModule;
import net.amoebaman.utils.ChatUtils;
import net.amoebaman.utils.ChatUtils.ColorScheme;
import net.amoebaman.utils.StatusBarAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RecurringOps implements Runnable {

	private static boolean announcedMinute;
	
	public void run() {
		if(GameMaster.debugCycle)
			GameMaster.logger().info("Printing debug information for one cycle of recurring ops");
		/*
		 * Enforce the task lock
		 */
//		World world = GameMaster.status.active ? GameMaster.activeMap.properties.getWorld("world") : GameMaster.mainLobby.getWorld();
//		world.setTime(GameMaster.worldTimeLock);
		/*
		 * Debug cycle info
		 */
		if(GameMaster.debugCycle){
			GameMaster.logger().info("Games: " + GameMaster.games);
			GameMaster.logger().info("Maps: " + GameMaster.maps);
			GameMaster.logger().info("Players: " + GameMaster.players);
			GameMaster.logger().info("Damages: " + GameMaster.lastDamage);
			GameMaster.logger().info("Respawning: " + GameMaster.respawning);
		}
		/*
		 * If the game is running...
		 */
		if(GameMaster.status == MasterStatus.RUNNING){
			/*
			 * Team game ops
			 */
			if(GameMaster.activeGame instanceof TeamAutoGame){

				TeamAutoGame game = (TeamAutoGame) GameMaster.activeGame;
				
				for(Player player : GameMaster.getPlayers())
					if(game.getTeam(player) == null){
						game.removePlayer(player);
						game.addPlayer(player);
					}
				
				if(TeamAutoGame.balancing)
					game.balanceTeams();
			}
			/*
			 * Safe spawn ops
			 */
			if(GameMaster.activeGame instanceof SafeSpawnModule){
				SafeSpawnModule game = (SafeSpawnModule) GameMaster.activeGame;
				
				for(Player player : GameMaster.getPlayers()){
					if(game.getRespawnLoc(player) == null){
						GameMaster.activeGame.removePlayer(player);
						GameMaster.activeGame.addPlayer(player);
					}
					if(player.getLocation().distance(game.getRespawnLoc(player)) < game.getSpawnRadius(player)){
						if(System.currentTimeMillis() - GameMaster.lastDamage.get(player) < 1000 * game.getSpawnReentryDelaySeconds(player)){
							player.damage(1);
							player.setVelocity(player.getLocation().clone().toVector().subtract(game.getRespawnLoc(player).clone().toVector()).multiply(0.25));
							player.sendMessage(ChatColor.RED + "You can't re-enter spawn for " + (game.getSpawnReentryDelaySeconds(player) - ((System.currentTimeMillis() - GameMaster.lastDamage.get(player)) / 1000) ) + " more seconds");
						}
						else
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 35, 6), true);
					}
				}
			}
			/*
			 * Timer ops
			 */
			if(GameMaster.activeGame instanceof TimerModule){
				TimerModule game = (TimerModule) GameMaster.activeGame;
				
				long millis = game.getGameLengthMinutes() * 60 * 1000 - (System.currentTimeMillis() - GameMaster.gameStart);
				int seconds = Math.round(millis / 1000F);
				int mins = seconds / 60;
				StatusBarAPI.setAllStatusBars(ChatUtils.format("[[" + GameMaster.activeGame.getGameName() + "]] on [[" + GameMaster.activeMap.name + "]] - [[" + mins + ":" + (seconds % 60 < 10 ? "0" + (seconds % 60) : seconds % 60) + "]]", ColorScheme.HIGHLIGHT), 1.0f * seconds / (game.getGameLengthMinutes() * 60));
				
				if(millis <= 1000)
					game.end();
				else if(seconds % 60 == 0 && mins > 0){
					if(!announcedMinute){
						Bukkit.broadcastMessage(ChatUtils.format("[[" + mins + " minutes]] remain on the clock", ColorScheme.HIGHLIGHT));
						announcedMinute = true;
					}}
				else
					announcedMinute = false;
			}
		}
		else{
			
		}
		/*
		 * Update player names
		 */
		if(GameMaster.debugCycle)
			GameMaster.logger().info("Updating player colors");
		GameMaster.updatePlayerColors();
		/*
		 * Turn off the debug cycle
		 */
		if(GameMaster.debugCycle)
			GameMaster.logger().info("Debug is finished");
		GameMaster.debugCycle = false;
	}
	
}
