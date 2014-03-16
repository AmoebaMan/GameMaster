package net.amoebaman.gamemasterv3;

import java.util.ArrayDeque;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.kitteh.tag.TagAPI;

import net.amoebaman.gamemasterv3.api.AutoGame;
import net.amoebaman.gamemasterv3.api.GameMap;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.kitmaster.Actions;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;


public class Progression{
	
	private AutoGame forcedNextGame;
	private Queue<AutoGame> gameHistory = new ArrayDeque<AutoGame>();
	private GameMap forcedNextMap;
	private Queue<GameMap> mapHistory = new ArrayDeque<GameMap>();
	
	private int intermission = 45, gameVoting = 15, mapVoting = 15;
	
	private GameMaster master;
	
	protected Progression(GameMaster master){
		this.master = master;
		intermission = master.getConfig().getInt("timing.intermission", 45);
		gameVoting = master.getConfig().getInt("timing.game-voting", 15);
		mapVoting = master.getConfig().getInt("timing.map-voting", 15);
	}
	
	public int getIntermissionLength(){
		return intermission;
	}
	
	protected AutoGame getNextGame(){
		return forcedNextGame;
	}
	
	protected Queue<AutoGame> getGameHistory(){
		return gameHistory;
	}
	
	protected GameMap getNextMap(){
		return forcedNextMap;
	}
	
	protected Queue<GameMap> getMapHistory(){
		return mapHistory;
	}
	
	protected void setNextGame(AutoGame nextGame){
		forcedNextGame = nextGame;
	}
	
	protected void setNextMap(GameMap nextMap){
		forcedNextMap = nextMap;
	}
	
	public void intermission(){
		/*
		 * Save the server
		 */
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
		/*
		 * Reset the world status
		 */
		World world = master.getLobby().getWorld();
		world.setStorm(false);
		world.setThundering(false);
		/*
		 * Purge the scoreboard
		 */
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		for(OfflinePlayer player : board.getPlayers())
			board.resetScores(player);
		for(Team team : board.getTeams())
			team.unregister();
		/*
		 * Repair the world
		 */
		master.repairWorld();
		/*
		 * Reset players and drag them to the lobby
		 */
		for(Player player : master.getPlayers()){
			master.getPlayerManager().resetPlayer(player);
			Actions.clearKits(player); // KitMaster hook
			player.teleport(master.getLobby());
		}
		/*
		 * Increment community stat if this was a big game
		 */
		if(master.getPlayers().size() >= 16)
			StatMaster.getHandler().incrementCommunityStat("big games");
		/*
		 * Update histories and such
		 */
		if(master.getActiveGame() != null){
			gameHistory.offer(master.getActiveGame());
			if(gameHistory.size() > 2)
				gameHistory.poll();
			mapHistory.offer(master.getActiveMap());
			if(mapHistory.size() > 5)
				mapHistory.poll();
			master.setActiveGame(null);
			master.setActiveMap(null);
		}
		/*
		 * Broadcast status
		 */
		Chat.broadcast(new Message(Scheme.HIGHLIGHT).t("The next game will start in ").t(intermission + " seconds").s(), new Message(Scheme.HIGHLIGHT).t("Voting on the next game is open for ").t(gameVoting + " seconds").s(), new Message(Scheme.HIGHLIGHT).t("CLICK HERE").s().command("/vote").t(" to vote on the next game"));
		/*
		 * Stamp intermission start
		 */
		master.stampGameStart();
		/*
		 * Schedule the next phase
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
			
			public void run(){
				endGameVoting();
			}
		}, gameVoting * 20);
	}
	
	private void endGameVoting(){
		/*
		 * Choose the game (at random if no votes were cast)
		 */
		AutoGame mostVoted = master.getGame(master.getMostVoted());
		if(mostVoted == null){
			master.setActiveGame(GenUtil.getRandomElement(master.getGames()));
			new Message(Scheme.HIGHLIGHT).then("No votes were cast - randomly choosing a game").broadcast();
		}
		else
			master.setActiveGame(mostVoted);
		/*
		 * Override that above if a game was determined manually
		 */
		if(forcedNextGame != null)
			master.setActiveGame(forcedNextGame);
		/*
		 * Broadcast
		 */
		Chat.broadcast(new Message(Scheme.HIGHLIGHT).t(master.getActiveGame()).s().t(" will start in ").t((intermission - gameVoting) + " seconds").s(), new Message(Scheme.HIGHLIGHT).t("Voting on the next map is open for ").t(mapVoting + " seconds").s(), new Message(Scheme.HIGHLIGHT).t("CLICK HERE").s().command("/vote").t(" to vote on the next map"));
		/*
		 * Schedule the next phase
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
			
			public void run(){
				endMapVoting();
			}
		}, mapVoting * 20);
	}
	
	private void endMapVoting(){
		/*
		 * Choose the map (at random if no vote were cast)
		 */
		GameMap mostVoted = master.getMap(master.getMostVoted());
		if(mostVoted == null){
			master.setActiveMap(GenUtil.getRandomElement(master.getMaps()));
			new Message(Scheme.HIGHLIGHT).then("No votes were cast - randomly choosing a map").broadcast();
		}
		else
			master.setActiveMap(mostVoted);
		/*
		 * Override that above if a game was determined manually
		 */
		if(forcedNextMap != null)
			master.setActiveMap(forcedNextMap);
		/*
		 * Broadcast
		 */
		Chat.broadcast(new Message(Scheme.HIGHLIGHT).t(master.getActiveGame()).s().t(" on ").t(master.getActiveMap()).s().t(" will start in ").t((intermission - gameVoting) + " seconds").s(), new Message(Scheme.HIGHLIGHT).t("Prepare for battle"));
		Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
			
			public void run(){
				startNextGame();
			}
		}, (intermission - gameVoting - mapVoting) * 20);
	}
	
	private void startNextGame(){
		/*
		 * Update master status
		 */
		master.setState(GameState.RUNNING);
		/*
		 * Start the game
		 */
		master.getActiveGame().start();
		master.stampGameStart();
		/*
		 * Reset player tags a second later to prevent invisi-glitches
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
			
			public void run(){
				
				for(Player player : master.getPlayers())
					TagAPI.refreshPlayer(player);
				
			}
		}, 20);
	}
	
}
