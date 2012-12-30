package com.amoebaman.gamemaster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.tag.TagAPI;

import com.amoebaman.gamemaster.api.AutoGame;
import com.amoebaman.gamemaster.api.GameMap;
import com.amoebaman.gamemaster.api.GameMap.Time;
import com.amoebaman.gamemaster.api.GameMap.Weather;
import com.amoebaman.gamemaster.api.TeamAutoGame;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.enums.PlayerStatus;
import com.amoebaman.gamemaster.handlers.ChargeHandler;
import com.amoebaman.gamemaster.objects.OfflinePlayerMap;
import com.amoebaman.gamemaster.objects.PlayerMap;
import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.ChatUtils;
import com.amoebaman.gamemaster.utils.ListUtils;
import com.amoebaman.gamemaster.utils.S_Location;
import com.amoebaman.gamemaster.utils.Utils;
import com.amoebaman.pvpstattracker.StatHandler;
import com.amoebaman.pvpstattracker.Statistic;
import com.google.common.collect.Lists;

public class GameMaster extends JavaPlugin{
	
	public static Logger logger;
	
	protected static String mainDirectory, gamesDirectory;
	protected static File configFile, chargeFile;
	
	public static long lastBroadcast, timeLock;
	public static int taskID;
	
	public static Location waitingRoom;
	public static MasterStatus status;
	
	public static AutoGame activeGame, nextGame, lastGame;
	public static GameMap activeMap, nextMap;
	
	public static final PlayerMap<PlayerStatus> players = new PlayerMap<PlayerStatus>(PlayerStatus.PLAYING);
	public static final PlayerMap<Long>	awaitingRespawn = new PlayerMap<Long>(0L);
	public static final PlayerMap<Boolean> teamChat = new PlayerMap<Boolean>(false);
	public static final OfflinePlayerMap<Integer> charges = new OfflinePlayerMap<Integer>();
	
	public static final UniqueList<Player> recentlyDamaged = new UniqueList<Player>();
	public static final UniqueList<GameMap> recentMaps = new UniqueList<GameMap>();
	
	public static final HashMap<CommandSender, String> votes = new HashMap<CommandSender, String>();

	private static UniqueList<AutoGame> events;
	
	public static final boolean DEBUG = false;
	public static boolean DEBUG_CYCLE = false;
	
	@Override
	public void onEnable(){
		logger = getLogger();
		/*
		 * Gobble up and store game plugins
		 */
		events = new UniqueList<AutoGame>();
		for(Plugin plugin : Bukkit.getPluginManager().getPlugins())
			if(plugin instanceof AutoGame)
				events.add((AutoGame) plugin);
		/*
		 * Define directories and files
		 */
		mainDirectory = getDataFolder().getPath();
		gamesDirectory = mainDirectory + "/games";
		new File(mainDirectory).mkdirs();
		new File(gamesDirectory).mkdirs();
		configFile = new File(mainDirectory + "/config.yml");
		chargeFile = new File(mainDirectory + "/charges.yml");
		/*
		 * Load up configuration and files
		 */
		try{
			if(!configFile.exists())
				configFile.createNewFile();
			getConfig().load(configFile);
			waitingRoom = S_Location.configLoad(getConfig().getConfigurationSection("waiting-room"));
			
			if(!chargeFile.exists())
				chargeFile.createNewFile();
			ChargeHandler.load(chargeFile);
		}
		catch(Exception e){ e.printStackTrace(); }
		/*
		 * Place records for all online players
		 */
		for(Player player : Bukkit.getOnlinePlayers()){
			if(player.hasPermission("gamemaster.autoexit"))
				players.put(player, PlayerStatus.NOT_PLAYING);
			else
				players.put(player, PlayerStatus.PLAYING);
		}
		/*
		 * Register events and commands
		 */
		GameMasterListener.init(this);
		GameMasterExecutor.init(this);
		/*
		 * Register game statistics with the StatHandler
		 */
		StatHandler.registerStat(new Statistic("Total Wins", new String[]{"games", "default"}, int.class, 0));
		StatHandler.registerStat(new Statistic("Total Losses", new String[]{"games", "default"}, int.class, 0));
		/*
		 * Schedule recurring ops
		 */
		taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			
			if(DEBUG_CYCLE)
				logger.info("Debugging GameMaster recurring ops");
			
			//Enforce the time lock
			World currentWorld = !status.isActive ? waitingRoom.getWorld() : activeGame.getActiveMap().world;
			currentWorld.setTime(timeLock);
			if(DEBUG_CYCLE){
				logger.info("Time lock set at " + timeLock);
				logger.info("MasterStatus: " + status);
				if(players == null)
					logger.info("players was null");
				else{
					logger.info("defaultValue for players: " + players.getDefaultValue());
					logger.info("players: " + players.toString());
				}
				if(awaitingRespawn == null)
					logger.info("awaitingRespawn was null");
				else{
					logger.info("defaultValue for awaitingRespawn: " + awaitingRespawn.getDefaultValue());
					logger.info("awaitingRespawn: " + awaitingRespawn.toString());
				}
				if(recentlyDamaged == null)
					logger.info("recentlyDamaged was null");
				else{
					logger.info("recentlyDamaged: " + recentlyDamaged.toString());
				}
			}
			
			//If the game is running...
			if(status == MasterStatus.RUNNING){
				
				if(DEBUG_CYCLE)
					logger.info("Performing ops on players...");
				
				//For each player...
				for(Player player : Bukkit.getOnlinePlayers()){
					
					//If they're closest to the waiting room, they're probably awaiting a respawn
					if(status == MasterStatus.RUNNING && awaitingRespawn.containsKey(player))
						players.put(player, PlayerStatus.RESPAWNING);
					
					if(DEBUG_CYCLE)
						logger.info("Status for player " + player.getName() + ": " + players.get(player));
					
					//Depending on their status...
					switch(players.get(player)){
					
					case PLAYING:
						
						//If they're inside their spawn and have been damaged recently...
						if(Utils.spawnDistance(player) < 7 && recentlyDamaged.contains(player) && activeGame instanceof TeamAutoGame){
							
							//Damage them a bit and kick them back out
							player.damage(2);
							player.setVelocity(player.getLocation().clone().toVector().subtract(activeGame.getRespawnLoc(player).clone().toVector()).multiply(0.25));
							player.sendMessage(ChatColor.RED + "You can't re-enter spawn for 10 seconds after taking damage");
						}
						
						/*
						//For every kill zone center...
						for(Location loc : activeMap.killZone)
							
							//If the player is inside it, kill them
							if(Utils.distance(player, loc) < 5){
								player.setHealth(0);
								Utils.sendToPlayer(player, new String[]{ChatUtils.ERROR + "You have exited the map boundaries"});
							}
						*/
						
						break;
						
					case RESPAWNING:
						
						//If their respawn time (game-defined) has expired...
						if(System.currentTimeMillis() - awaitingRespawn.get(player) > activeGame.getRespawnSeconds() * 1000){
							
							//Send them to their spawn point and reset their status
							player.teleport(activeGame.getRespawnLoc(player));
							List<String> message = activeGame.getSpawnMessage(player);
							if(message != null && !message.isEmpty())
								Utils.message(player, activeGame.getSpawnMessage(player));
							Utils.resetPlayerStatus(player, false);
							awaitingRespawn.remove(player);
							players.put(player, PlayerStatus.PLAYING);
						}
						break;
					default: }
				}
			}

			//Update all the team colors
			Utils.updatePlayerColors();
			
			//Switch off the debug cycler
			DEBUG_CYCLE = false;
		} }, 10L, 10L);
		/*
		 * Start the works
		 */
		enterIntermission();
	}
	
	public void onDisable(){
		if(status.isActive)
			activeGame.endGame(null);
		Bukkit.getScheduler().cancelTask(taskID);
		try{
			ChargeHandler.save(chargeFile);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	
	/**
	 * Gets a unique list of all players whose status matches the argument
	 * @param status The PlayerStatus to match
	 * @return The list of players
	 */
	public static UniqueList<Player> getPlayers(PlayerStatus status){
		UniqueList<Player> toReturn = new UniqueList<Player>();
		for(Player player : players.keySet())
			if(players.get(player) == status || (status == PlayerStatus.PLAYING && players.get(player) == PlayerStatus.RESPAWNING))
				toReturn.add(player);
		return toReturn;
	}
	
	//Methods to handle game flow
	
 	public static void enterIntermission(){
 		
 		//Save the server
 		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
 		
 		if(status == MasterStatus.INTERMISSION)
 			return;
 		
		status = MasterStatus.INTERMISSION;
		
		//Reset the world's status
		timeLock = 6000;
		waitingRoom.getWorld().setStorm(false);
		waitingRoom.getWorld().setThundering(false);
		
		//Reset the players and bring them to the waiting room
		for(Player player : getPlayers(PlayerStatus.PLAYING)){
			Utils.resetPlayerStatus(player, true);
			player.teleport(waitingRoom);
			if(players.get(player) == PlayerStatus.RESPAWNING)
				players.put(player, PlayerStatus.PLAYING);
		}
		awaitingRespawn.clear();
		
		//Update the actives and pasts
		if(activeGame != null){
			lastGame = activeGame;
			recentMaps.add(activeMap);
			if(recentMaps.size() > 3)
				recentMaps.remove(0);
			activeGame = null;
			activeMap = null;
		}
		
		//Broadcast
		Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "We'll start the next game in one minute");
		Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "Vote on the next game with " + ChatUtils.highlightEmphasis("/vote <game>"));
		
		//In 60 seconds (1200 ticks) move on to the next phase
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable(){ public void run(){
			enterMapVoting();
		} }, DEBUG ? 100 : 1200);
	}
	
	private static void enterMapVoting(){
		status = MasterStatus.PREP;
		
		//If no votes were cast, choose a random game
		if(votes.isEmpty()){
			activeGame = events.getRandom();
			Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "No votes were cast, randomly selecting the next game");
		}
		
		//Otherwise...
		else{
			
			//Translate the map of player:vote into vote:frequency
			HashMap<String, Integer> tally = new HashMap<String, Integer>();
			for(String vote : votes.values())
				tally.put(vote, ListUtils.instances(votes.values(), vote));
			
			//Get the most voted for game
			int maxVotes = 0;
			String mostVoted = null;
			for(String game : tally.keySet())
				if(tally.get(game) > maxVotes){
					mostVoted = game;
					maxVotes = tally.get(game);
				}
			if(mostVoted != null)
				activeGame = getRegisteredGame(mostVoted);
			
			//If something goes horribly wrong, just pick a random game
			else{
				activeGame = events.getRandom();
				Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "An error occurred while tallying, randomly selecting the next game");
			}
			
			//Clear the votes so people can vote on maps
			votes.clear();
		}
		
		//If an admin set the next game, this overrides the vote
		if(nextGame != null){
			activeGame = nextGame;
			nextGame = null;
		}
		
		//Again, if something goes horribly wrong, just pick a random game
		if(activeGame == null){
			activeGame = events.getRandom();
			Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "An error occurred selecting game, randomly selecting the next game");
		}
		
		//Broadcast
		Utils.broadcast(Lists.newArrayList(new String[]{	
				ChatUtils.HIGHLIGHT + "The next game will be " + ChatUtils.highlightEmphasis(activeGame.getGameName()),
				ChatUtils.HIGHLIGHT + "Use " + ChatUtils.highlightEmphasis("/help games") + " for more information",
				ChatUtils.HIGHLIGHT + "Vote on the next map with " + ChatUtils.highlightEmphasis("/vote <map>"),
				ChatUtils.HIGHLIGHT + "The game will start in 30 seconds",
		}));
		
		//After 30 seconds (600 ticks) start the game
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable(){ public void run(){
			startNextGame();
		} }, DEBUG ? 100 : 600);
	}
	
	public static void startNextGame(){
		status = MasterStatus.RUNNING;

		//If no votes were cast, pick a random map
		if(votes.size() == 0){
			activeMap = activeGame.getMaps().getRandom();
			Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "No votes were cast, randomly selecting the next map");
		}
		
		//Otherwise...
		else{
			
			//Translate the map of player:vote into vote:frequency
			HashMap<String, Integer> tally = new HashMap<String, Integer>();
			for(String vote : votes.values())
				tally.put(vote, ListUtils.instances(votes.values(), vote));
			
			//Get the most voted for game
			int maxVotes = 0;
			String mostVoted = null;
			for(String event : tally.keySet())
				if(tally.get(event) > maxVotes){
					mostVoted = event;
					maxVotes = tally.get(event);
				}
			if(mostVoted != null)
				activeMap = activeGame.getMap(mostVoted);
			
			//If something goes horribly wrong, just pick a random game
			else{
				activeMap = activeGame.getMaps().getRandom();
				Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "An error occurred while tallying, randomly selecting the next map");
			}
			
			//Clear the votes for next time around
			votes.clear();
		}
		
		//If an admin has preset the next map, this overrides the vote
		if(nextMap != null){
			activeMap = nextMap;
			nextMap = null;
		}
		
		//Again, if something goes horribly wrong, just pick a random game
		if(activeMap == null){
			activeMap = activeGame.getMaps().getRandom();
			Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + "An error occurred selecting map, randomly selecting the next map");
		}
		
		//Let the chosen game set itself up
		activeGame.prepGame();
		activeMap = activeGame.getActiveMap();
		
		//Handle time and weather conditions for the map
		if(activeMap.time == Time.RANDOM)
			activeMap.time = Time.getRandom();
		if(activeMap.weather == Weather.RANDOM)
			activeMap.weather = Weather.getRandom();
		timeLock = activeMap.time.ticks;	
		if(activeMap.weather == Weather.STORMING)
			activeMap.world.setThundering(true);
		if(activeMap.weather == Weather.RAINING)
			activeMap.world.setStorm(true);
		
		//Let the chosen game start itself
		activeGame.startGame();
		
		//2 seconds (40 ticks) later, refresh the players' colored name tags
		Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable(){ public void run(){
			HashSet<Player> setPlayers = new HashSet<Player>();
			setPlayers.addAll(getPlayers(PlayerStatus.PLAYING));
			for(Player player : setPlayers){
				try{ TagAPI.refreshPlayer(player, setPlayers);}
				catch(Exception e){}
			}
		} }, 40);
	}
	
	//Methods to handle game registration and deregistration
	
	public static GameMaster registerGame(AutoGame game){
		if(game == null){
			logger.info("Failed to register game: null game");
			return null;
		}
		if(game.getMaps().size() < 1){
			logger.info("Failed to register game: no maps");
			return null;
		}
		if(!events.contains(game)){
			events.add(game);
			logger.info("Registered game named " + game.getGameName());
		}
		return getPlugin();
	}
	
	public static void deregisterGame(AutoGame event){
		events.remove(event);
		logger.info("Deregistered game named " + event.getGameName());
	}
	
	public static GameMaster getPlugin(){ return (GameMaster) Bukkit.getPluginManager().getPlugin("GameMaster"); }
	
	public static AutoGame getRegisteredGame(String str){
		str = str.replaceAll(" ", "-");
		for(AutoGame event : events){
			if(event.getName().equalsIgnoreCase(str) || event.getName().toLowerCase().startsWith(str.toLowerCase()))
				return event;
			for(String alias : event.getAliases())
				if(alias.equalsIgnoreCase(str) || alias.toLowerCase().startsWith(str.toLowerCase()))
					return event;
		}
		return null;
	}
	
	public static UniqueList<AutoGame> getRegisteredGames(){
		UniqueList<AutoGame> filtered = new UniqueList<AutoGame>();
		for(AutoGame game : events)
			if(game.getMaps() != null && !game.getMaps().isEmpty())
				filtered.add(game);
		return filtered;
	}

	public static String getDirectory(AutoGame game){
		String path = gamesDirectory + "/" + game.getDescription().getName();
		new File(path).mkdirs();
		return path;
	}
	
}
