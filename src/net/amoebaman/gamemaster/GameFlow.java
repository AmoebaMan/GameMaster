package net.amoebaman.gamemaster;

import java.util.ArrayList;
import java.util.List;

import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.enums.Team;
import net.amoebaman.gamemaster.modules.MessagerModule;
import net.amoebaman.kitmaster.Actions;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.chat.Align;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Scheme;
import net.amoebaman.utils.chat.JsonMessage;
import net.amoebaman.utils.chat.Message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
		Chat.broadcast(new Message(Scheme.HIGHLIGHT).then("The next game will start in ").then("one minute").strong());
		if(GameMaster.games.size() > 1)
			Chat.broadcast(new JsonMessage(Scheme.HIGHLIGHT).then("Click here").strong().style(ChatColor.BOLD).command("/vote").then(" to vote for the next game"));
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
			GameMaster.activeGame = GenUtil.getRandomElement(GameMaster.games);
			Chat.broadcast(new Message(Scheme.HIGHLIGHT).then("No votes were cast").alternate().then(" - randomly choosing a game"));
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
			if(GameMaster.activeGame == null)
				GameMaster.activeGame = GenUtil.getRandomElement(GameMaster.games);
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
		if(GameMaster.activeGame == null)
			GameMaster.activeGame = GenUtil.getRandomElement(GameMaster.games);
		/*
		 * Update status
		 */
		GameMaster.status = MasterStatus.PREP;
		/*
		 * Broadcast
		 */
		Chat.broadcast(
				new Message(Scheme.HIGHLIGHT).then("The next game will be ").then(GameMaster.activeGame).strong().toString(),
				new Message(Scheme.HIGHLIGHT).then("The game will start in ").then("30 seconds").strong().toString(),
				new JsonMessage(Scheme.HIGHLIGHT).then("Click here").strong().style(ChatColor.BOLD).command("/vote").then(" to vote for the next map")
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
			GameMaster.activeMap = GenUtil.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
			Chat.broadcast(new Message(Scheme.HIGHLIGHT).then("No votes were cast").alternate().then(" - randomly choosing a map"));
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
			if(GameMaster.activeMap == null)
				GameMaster.activeMap = GenUtil.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
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
		if(GameMaster.activeMap == null)
			GameMaster.activeMap = GenUtil.getRandomElement(GameMaster.getCompatibleMaps(GameMaster.activeGame));
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
				
				List<String> messages = new ArrayList<String>();
				if(GameMaster.activeGame instanceof TeamAutoGame){
					Team team = ((TeamAutoGame) GameMaster.activeGame).getTeam(player);
					messages.add(new Message(Scheme.HIGHLIGHT).then("You are on the ").then(team).color(team.chat).then(" team").toString());
				}
				if(GameMaster.activeGame instanceof MessagerModule)
					messages.addAll( ((MessagerModule) GameMaster.activeGame).getWelcomeMessage(player));

				Chat.send(player, Align.box(messages, ""));
			}
		} }, 40);
	}
	
}
