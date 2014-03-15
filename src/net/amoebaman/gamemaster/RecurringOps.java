package net.amoebaman.gamemaster;

import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.modules.SafeSpawnModule;
import net.amoebaman.gamemaster.modules.TimerModule;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Scheme;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.nms.StatusBar;

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
					if(game.getSafeLoc(player) == null){
						GameMaster.activeGame.removePlayer(player);
						GameMaster.activeGame.addPlayer(player);
					}
					if(player.getLocation().distance(game.getSafeLoc(player)) < game.getSafeRadius(player)){
						if(System.currentTimeMillis() - GameMaster.lastDamage.get(player) < 1000 * game.getSafeReentryTimeout(player)){
							player.damage(1);
							player.setVelocity(player.getLocation().clone().toVector().subtract(game.getSafeLoc(player).clone().toVector()).multiply(0.25));
							player.sendMessage(ChatColor.RED + "You can't re-enter spawn for " + (game.getSafeReentryTimeout(player) - ((System.currentTimeMillis() - GameMaster.lastDamage.get(player)) / 1000) ) + " more seconds");
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
				
				long millis = game.getGameLength() * 60 * 1000 - (System.currentTimeMillis() - GameMaster.gameStart);
				int seconds = Math.round(millis / 1000F);
				int mins = seconds / 60;
				String status = new Message(Scheme.HIGHLIGHT).then(GameMaster.activeGame).strong().then(" on ").then(GameMaster.activeMap).strong().then(" - ").then(mins + ":" + (seconds % 60 < 10 ? "0" + (seconds % 60) : seconds % 60)).strong().toString();
				StatusBar.setAllStatusBars(status, 1.0f * seconds / (game.getGameLength() * 60), 1);
				
				if(millis <= 1000)
					game.end();
				else if(seconds % 60 == 0 && mins > 0){
					if(!announcedMinute){
						Chat.broadcast(new Message(Scheme.HIGHLIGHT).then(mins + " minutes").strong().then(" remain on the clock"));
						announcedMinute = true;
					}}
				else
					announcedMinute = false;
			}
		}
		else{
			StatusBar.removeAllStatusBars();
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
