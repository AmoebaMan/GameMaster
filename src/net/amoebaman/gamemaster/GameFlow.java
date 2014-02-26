package net.amoebaman.gamemaster;

import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.enums.Team;
import net.amoebaman.gamemaster.modules.MessagerModule;
import net.amoebaman.utils.ChatUtils;
import net.amoebaman.utils.ChatUtils.ColorScheme;
import net.amoebaman.gamemaster.utils.Utils;
import net.amoebaman.kitmaster.Actions;
import net.amoebaman.statmaster.StatMaster;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public class GameFlow {
	
	public static void startIntermission(){
		/*
		 * Save the server
		 */
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
		/*
		 * If we're already in the intermission, stop and return
		 * We need this to prevent two games from running simultaneously
		 */
		if(GameMaster.status == MasterStatus.INTERMISSION)
			return;
		GameMaster.status = MasterStatus.INTERMISSION;
		/*
		 * Reset the world's status
		 */
		GameMaster.worldTimeLock = 6000;
		GameMaster.mainLobby.getWorld().setStorm(false);
		GameMaster.mainLobby.getWorld().setThundering(false);
		/*
		 * Purge the scoreboard
		 */
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		for(OfflinePlayer player : board.getPlayers())
			board.resetScores(player);
		/*
		 * Repair
		 */
		GameMaster.repair();
		/*
		 * Reset all players and drag them to the lobby
		 */
		for(Player player : GameMaster.getPlayers()){
			GameMaster.resetPlayer(player);
			Actions.clearKits(player);
			player.teleport(GameMaster.mainLobby);
		}
		/*
		 * Increment community stat if this was a big game
		 */
		if(GameMaster.getPlayers().size() >= 16)
			StatMaster.getHandler().incrementCommunityStat("big games");
		/*
		 * Update games
		 */
		if(GameMaster.activeGame != null){
			GameMaster.lastGame = GameMaster.activeGame;
			GameMaster.mapHistory.add(GameMaster.activeMap);
			if(GameMaster.mapHistory.size() > 5)
				GameMaster.mapHistory.remove(0);
			GameMaster.activeGame = null;
			GameMaster.activeMap = null;
		}
		/*
		 * Broadcast status
		 */
		Bukkit.broadcastMessage(ChatUtils.format("We'll start the next game in [[one minute]]", ColorScheme.HIGHLIGHT));
		if(GameMaster.games.size() > 1)
			Bukkit.broadcastMessage(ChatUtils.format("Vote for the next game with [[/vote <game>]]", ColorScheme.HIGHLIGHT));
		/*
		 * Schedule the next phase
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
			startMapVoting();
		} }, 1200);
	}
	
	public static void startMapVoting(){
		/*
		 * Choose a random game if no votes were cast
		 */
		if(GameMaster.votes.isEmpty()){
			GameMaster.activeGame = Utils.getRandomElement(GameMaster.games);
			Bukkit.broadcastMessage(ChatUtils.format("No votes were cast, randomly selecting the next game", ColorScheme.HIGHLIGHT));
		}
		/*
		 * Otherwise...
		 */
		else{
			/*
			 * Translate into vote-to-frequency
			 */
			GameMaster.activeGame = GameMaster.getRegisteredGame(GameMaster.getMostVoted());
			/*
			 * If something has gone wrong, choose a random game
			 */
			if(GameMaster.activeGame == null){
				GameMaster.activeGame = Utils.getRandomElement(GameMaster.games);
				Bukkit.broadcastMessage(ChatUtils.format("An error occurred while tallying votes, choosing a random game", ColorScheme.ERROR));
			}
			/*
			 * Clear the votes
			 */
			GameMaster.votes.clear();
		}
		/*
		 * Admin-set games override the vote
		 */
		if(GameMaster.nextGame != null){
			GameMaster.activeGame = GameMaster.nextGame;
			GameMaster.nextGame = null;
		}
		/*
		 * If something has gone wrong, choose a random game
		 */
		if(GameMaster.activeGame == null){
			GameMaster.activeGame = Utils.getRandomElement(GameMaster.games);
			Bukkit.broadcastMessage(ChatUtils.format("An error occurred while selecting the game, choosing a random game", ColorScheme.ERROR));
		}
		/*
		 * Update status
		 */
		GameMaster.status = MasterStatus.PREP;
		/*
		 * Broadcast
		 */
		ChatUtils.bigBroadcast(ColorScheme.HIGHLIGHT,
				"The next game will be [[" + GameMaster.activeGame + "]]",
				"Vote on the next map with [[/vote <map>]]",
				"The game will start in [[30 seconds]]"
				);
		/*
		 * Schedule the next phase
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
			startNextGame();
		} }, 600);
	}
	
	public static void startNextGame(){
		/*
		 * Choose a random map if no votes were cast
		 */
		if(GameMaster.votes.isEmpty()){
			GameMaster.activeMap = Utils.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
			Bukkit.broadcastMessage(ChatUtils.format("No votes were cast, randomly selecting the next map", ColorScheme.HIGHLIGHT));
		}
		/*
		 * Otherwise...
		 */
		else{
			/*
			 * Translate into vote-to-frequency
			 */
			GameMaster.activeMap = GameMaster.getRegisteredMap(GameMaster.getMostVoted());
			/*
			 * If something has gone wrong, choose a random map
			 */
			if(GameMaster.activeMap == null){
				GameMaster.activeMap = Utils.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
				Bukkit.broadcastMessage(ChatUtils.format("An error occurred while tallying votes, choosing a random map", ColorScheme.ERROR));
			}
			/*
			 * Clear the votes
			 */
			GameMaster.votes.clear();
		}
		/*
		 * Admin-set games override the vote
		 */
		if(GameMaster.nextMap != null){
			GameMaster.activeMap = GameMaster.nextMap;
			GameMaster.nextMap = null;
		}
		/*
		 * If something has gone wrong, choose a random map
		 */
		if(GameMaster.activeMap == null){
			GameMaster.activeMap = Utils.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
			Bukkit.broadcastMessage(ChatUtils.format("An error occurred while selecting the map, choosing a random map", ColorScheme.ERROR));
		}
		/*
		 * Configure the world's time and weather as per the map's instruction
		 */
//		World world = GameMaster.activeMap.properties.getWorld("world");
//		Time time = Time.matchString(GameMaster.activeMap.properties.getString("time"));
//		if(time == Time.RANDOM)
//			GameMaster.worldTimeLock = new Random().nextInt(24000);
//		else
//			GameMaster.worldTimeLock = time.ticks;
//		Weather weather = Weather.matchString(GameMaster.activeMap.properties.getString("weather"));
//		if(weather == Weather.RANDOM){
//			world.setStorm(Math.random() > 0.7);
//			if(world.hasStorm())
//				world.setThundering(Math.random() > 0.5);
//		}
//		else{
//			world.setStorm(weather == Weather.RAINING);
//			world.setThundering(weather == Weather.STORMING);
//		}
		/*
		 * Update status
		 */
		GameMaster.status = MasterStatus.RUNNING;
		/*
		 * Start the chosen game
		 */
		GameMaster.gameStart = System.currentTimeMillis();
		GameMaster.activeGame.start();
		/*
		 * 2 seconds later...
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
			/*
			 * Reset player tags to prevent general glitchiness
			 */
			for(Player player : GameMaster.getPlayers()){
				player.sendMessage(ChatUtils.spacerLine());
				if(GameMaster.activeGame instanceof TeamAutoGame){
					Team team = ((TeamAutoGame) GameMaster.activeGame).getTeam(player);
					player.sendMessage(ChatUtils.centerAlign(ChatUtils.format("You are on the " + team.chat + team + "]] team", ColorScheme.HIGHLIGHT)));
				}
				if(GameMaster.activeGame instanceof MessagerModule)
					for(String line : ((MessagerModule) GameMaster.activeGame).getWelcomeMessage(player))
						player.sendMessage(ChatUtils.centerAlign(ChatUtils.format(line, ColorScheme.NORMAL)));
				player.sendMessage(ChatUtils.spacerLine());
			}
		} }, 40);
	}
	
}
