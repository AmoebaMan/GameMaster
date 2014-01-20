package net.amoebaman.gamemaster;

import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.modules.SafeSpawnModule;
import net.amoebaman.gamemaster.modules.TimerModule;
import net.amoebaman.gamemaster.utils.ChatUtils;
import net.amoebaman.gamemaster.utils.ChatUtils.ColorScheme;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class RecurringOps implements Runnable {

	private static Scoreboard timerBoard = Bukkit.getScoreboardManager().getNewScoreboard();
	static{
		Objective timer = timerBoard.registerNewObjective("timer", "dummy");
		timer.setDisplayName(ChatColor.GOLD + "        -=[ Timer ]=-        ");
		timer.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
//	private static long lastScoreboardSwitch;
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
				
				/*
				 * Scoreboard timer
				 */
				timerBoard.getObjective("timer").getScore(Bukkit.getOfflinePlayer("Seconds left")).setScore(getSecondsRemaining(game));
//				if(System.currentTimeMillis() - lastScoreboardSwitch > 5000){
//					lastScoreboardSwitch = System.currentTimeMillis();
//					for(Player player : Bukkit.getOnlinePlayers()){
//						if(player.getScoreboard().getObjective("timer") != null)
//							player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
//						else
//							player.setScoreboard(timerBoard);
//					}
//				}
				
				if(getMillisRemaining(game) <= 1000){
					timerBoard.resetScores(Bukkit.getOfflinePlayer("Seconds left"));
					game.end();
				}
				else if(getSecondsRemaining(game) % 60 == 0 && getMinutesRemaining(game) > 0){
					if(!announcedMinute){
						Bukkit.broadcastMessage(ChatUtils.format("[[" + getMinutesRemaining(game) + " minutes]] remain on the clock", ColorScheme.HIGHLIGHT));
						announcedMinute = true;
					}
				}
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
	
	public static final long getMillisRemaining(TimerModule game){
		return game.getGameLengthMinutes() * 60 * 1000 - (System.currentTimeMillis() - GameMaster.gameStart);
	}
	
	public static final int getSecondsRemaining(TimerModule game){
		return Math.round(getMillisRemaining(game) / 1000F);
	}
	
	public static final int getMinutesRemaining(TimerModule game){
		return getSecondsRemaining(game) / 60;
	}
	
}
