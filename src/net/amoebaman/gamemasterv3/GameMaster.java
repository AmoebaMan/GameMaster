package net.amoebaman.gamemasterv3;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criterias;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.amoebaman.gamemasterv3.api.AutoGame;
import net.amoebaman.gamemasterv3.api.GameMap;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.statmaster.Statistic;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.S_Loc;
import net.amoebaman.utils.maps.PlayerMap;
import net.amoebaman.utils.maps.StringMap;

/**
 * The main class
 * 
 * @author AmoebaMan
 */
public class GameMaster extends JavaPlugin{
	
	private static GameMaster INSTANCE;
	
	private File configFile, mapsFile, repairFile;
	
	private StringMap<AutoGame> games = new StringMap<AutoGame>();
	private StringMap<GameMap> maps = new StringMap<GameMap>();
	
	private PlayerMap<PlayerState> players = new PlayerMap<PlayerState>(PlayerState.EXTERIOR);
	private PlayerMap<String> votes = new PlayerMap<String>("");
	
	private AutoGame activeGame;
	private GameMap activeMap;
	
	private Location lobby, fireworks, welcome;
	
	private int tickTaskId = 0;
	
	private GameState state = GameState.INTERMISSION;
	private long gameStart = 0L;
	
	private Progression progression;
	private Players playerManager;
	
	public void onEnable(){
		INSTANCE = this;
		/*
		 * Establish files and folders
		 */
		configFile = GenUtil.getConfigFile(this, "config");
		mapsFile = GenUtil.getConfigFile(this, "maps");
		repairFile = GenUtil.getConfigFile(this, "repair");
		/*
		 * Load configs
		 */
		try{
			/*
			 * The "proper" configuration
			 */
			getConfig().options().pathSeparator('/');
			getConfig().load(configFile);
			
			lobby = S_Loc.stringLoad(getConfig().getString("locations.lobby", "world@0.5,64,0,5"));
			fireworks = S_Loc.stringLoad(getConfig().getString("locations.fireworks", S_Loc.stringSave(lobby, true, false)));
			welcome = S_Loc.stringLoad(getConfig().getString("locations.welcome", S_Loc.stringSave(lobby, true, false)));
			/*
			 * The maps configuration
			 */
			YamlConfiguration mapsYaml = new YamlConfiguration();
			mapsYaml.options().pathSeparator('/');
			mapsYaml.load(mapsFile);
			for(String key : mapsYaml.getKeys(false)){
				ConfigurationSection sec = mapsYaml.getConfigurationSection(key);
				GameMap map = new GameMap(key);
				for(String prop : sec.getKeys(true))
					if(!sec.isColor(prop))
						map.getProperties().set(prop, sec.get(prop));
				registerMap(map);
			}
			/*
			 * Repair the map as necessar
			 */
			repairWorld();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*
		 * Update player states and such
		 */
		if(getConfig().getBoolean("wrap-server", false)){
			players = new PlayerMap<PlayerState>(PlayerState.PLAYING);
			for(Player each : Bukkit.getOnlinePlayers())
				if(each.hasPermission("gamemaster.admin"))
					setState(each, PlayerState.EXTERIOR);
				else
					setState(each, PlayerState.PLAYING);
		}
		/*
		 * Register statistics
		 */
		StatMaster.getHandler().registerStat(new Statistic("Wins", 0, "games", "default"));
		StatMaster.getHandler().registerStat(new Statistic("Losses", 0, "games", "default"));
		StatMaster.getHandler().registerCommunityStat(new Statistic("Big games", 0));
		StatMaster.getHandler().registerCommunityStat(new Statistic("Votes", 0));
		/*
		 * Start the ticker
		 */
		tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new GameTicker(this), 0, 5L);
		/*
		 * Do scoreboard stuff
		 */
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective health = board.getObjective("health");
		if(health == null)
			health = board.registerNewObjective("health", Criterias.HEALTH);
		health.setDisplaySlot(DisplaySlot.BELOW_NAME);
		health.setDisplayName(" HP");
		/*
		 * Set up components
		 */
		progression = new Progression(this);
		playerManager = new Players(this);
		/*
		 * Start the ball rolling
		 */
		progression.intermission();
	}
	
	public void onDisable(){
		/*
		 * Cancel the ticker
		 */
		Bukkit.getScheduler().cancelTask(tickTaskId);
		/*
		 * Save the configs
		 */
		try{
			getConfig().set("locations.lobby", S_Loc.stringSave(lobby, true, true));
			getConfig().set("locations.fireworks", S_Loc.stringSave(lobby, true, false));
			getConfig().set("locations.welcome", S_Loc.stringSave(welcome, true, true));
			
			YamlConfiguration mapYaml = new YamlConfiguration();
			mapYaml.options().pathSeparator('/');
			for(GameMap each : maps.values())
				mapYaml.createSection(each.getName(), each.getProperties().getValues(true));
			mapYaml.save(mapsFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*
		 * Repair the world
		 */
		repairWorld();
	}
	
	/**
	 * Gets the instance of the game master.
	 * 
	 * @return the instance
	 */
	public static GameMaster getMaster(){
		return INSTANCE;
	}
	
	
    protected StringMap<AutoGame> getGames(){
    	return games;
    }
    

	
    protected StringMap<GameMap> getMaps(){
    	return maps;
    }
    

	/**
	 * Registers an {@link AutoGame} with the master. If a game has already been
	 * registered with the same name, it will be overwritten.
	 * 
	 * @param game a game
	 * @return the instance of the game master, for the registering party's use
	 */
	public GameMaster registerGame(AutoGame game){
		if(game != null){
			games.put(game.getName(), game);
			getLogger().info("Loaded auto game named " + game.getName());
		}
		else
			getLogger().warning("Failed to register null game");
		return this;
	}
	
	/**
	 * Registers a {@link GameMap} with the master. If a map has already been
	 * registered with the same name, it will be overwritten.
	 * 
	 * @param map a map
	 */
	public void registerMap(GameMap map){
		if(map != null){
			maps.put(map.getName(), map);
			getLogger().info("Loaded game map named " + map.getName());
		}
		else
			getLogger().warning("Failed to register null map");
	}
	
	/**
	 * Removes an {@link AutoGame} from the master registry.
	 * 
	 * @param game a game
	 */
	public void deregisterGame(AutoGame game){
		games.remove(game.getName());
	}
	
	/**
	 * Removes a {@link GameMap} from the master registry.
	 * 
	 * @param map a map
	 */
	public void deregisterMap(GameMap map){
		maps.remove(map.getName());
	}
	
	/**
	 * Gets a game from the master registry.
	 * 
	 * @param name a game name
	 * @return the game, or null if none was found
	 */
	public AutoGame getGame(String name){
		return games.get(name);
	}
	
	/**
	 * Gets a game map from the master registry.
	 * 
	 * @param name a map name
	 * @return the map, or null if none was found
	 */
	public GameMap getMap(String name){
		return maps.get(name);
	}
	
	/**
	 * Gets all maps compatible with an {@link AutoGame}, determined using
	 * {@link AutoGame#isCompatible(GameMap)}.
	 * 
	 * @param game a game
	 * @return all compatible maps
	 */
	public Set<GameMap> getMaps(AutoGame game){
		Set<GameMap> set = new HashSet<GameMap>();
		for(GameMap map : maps.values())
			if(game.isCompatible(map))
				set.add(map);
		return set;
	}
	
	/**
	 * Stores a set of all the original forms of the blocks a game will change,
	 * so that they can be restored to their original status when the game ends.
	 * This list is saved directly to a flat file, so even if the server crashes
	 * abruptly, these blocks will still be restored when the server starts up.
	 * <p>
	 * For the sake of speed and simplicity, this system <b>will not</b> save
	 * any NBT data on the block; only the type ID and data value will be saved.
	 * This is for repairing <b>scenery</b>, not mechanical contraptions.
	 * 
	 * @param states
	 */
	@SuppressWarnings("deprecation")
	public void logRepair(Set<BlockState> states){
		/*
		 * Set up the YAML config
		 */
		YamlConfiguration repairYaml = new YamlConfiguration();
		repairYaml.options().pathSeparator('/');
		/*
		 * Save all the recorded block states
		 * For simplicity's sake, we're not making any attempt to store NBT
		 * stuff or any of that extra junk
		 */
		for(BlockState state : states)
			repairYaml.set(S_Loc.stringSave(state.getLocation(), true, false), state.getTypeId() + ":" + state.getRawData());
		
		try{
			repairYaml.save(repairFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the set of logged repair blocks from flat file, and restores them
	 * to their original states. See {@link #logRepair(Set)}.
	 * <p>
	 * This method is called automatically after every game is finished, and
	 * also when the game master is first loaded (in case the server crashed
	 * unexpectedly). As such, under normal circumstances games should not have
	 * to ever call this method directly.
	 */
	@SuppressWarnings("deprecation")
	public void repairWorld(){
		/*
		 * Load and reset the repair log
		 */
		YamlConfiguration repairYaml = new YamlConfiguration();
		repairYaml.options().pathSeparator('/');
		try{
			repairYaml.load(repairFile);
			repairFile.delete();
			repairFile.createNewFile();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*
		 * Restore all the blocks listed in the repair log
		 */
		for(String key : repairYaml.getKeys(false)){
			Block block = S_Loc.stringLoad(key).getBlock();
			String[] split = repairYaml.getString(key).split(":");
			if(block.getType() == Material.CHEST)
				((Chest) block.getState()).getBlockInventory().clear();
			block.setTypeIdAndData(Integer.parseInt(split[0]), Byte.parseByte(split[1]), false);
		}
	}
	
	/**
	 * Gets the player manager that works for this game master
	 * 
	 * @return the player manager
	 */
	public Players getPlayerManager(){
		return playerManager;
	}
	
	/**
	 * Gets the current state of the game. See {@link GameState}.
	 * 
	 * @return the state
	 */
	public GameState getState(){
		return state;
	}
	
	protected void setState(GameState state){
		this.state = state;
	}
	
	/**
	 * Gets the state of a player. See {@link PlayerState}.
	 * 
	 * @param player a player
	 * @return the player's state
	 */
	public PlayerState getState(Player player){
		return players.get(player);
	}
	
	protected void setState(Player player, PlayerState newState){
		/*
		 * Update the state on record
		 */
		if(newState == null)
			players.remove(player);
		else
			players.put(player, newState);
		/*
		 * Remove stamps as necessary
		 */
		playerManager.destamp(player);
		/*
		 * Join/leave the game/lobby
		 */
		if(state != GameState.INTERMISSION)
			if(newState == PlayerState.EXTERIOR)
				activeGame.leave(player);
			else
				activeGame.join(player);
		else
			if(newState != PlayerState.EXTERIOR)
				player.teleport(lobby);
		/*
		 * Update game modes
		 */
		if(newState == PlayerState.PLAYING)
			player.setGameMode(GameMode.SURVIVAL);
		if(newState == PlayerState.WATCHING)
			player.setGameMode(GameMode.CREATIVE);
	}
	
	/**
	 * Gets the game that is currently playing, or null if none is.
	 * 
	 * @return the game
	 */
	public AutoGame getActiveGame(){
		return activeGame;
	}
	
	/**
	 * Gets the map that is currently playing, or null if none is.
	 * 
	 * @return the map
	 */
	public GameMap getActiveMap(){
		return activeMap;
	}

	protected void setActiveGame(AutoGame activeGame){
		this.activeGame = activeGame;
	}
	
	protected void setActiveMap(GameMap activeMap){
		this.activeMap = activeMap;
	}
	
	/**
	 * Gets all the players currently participating with the game master,
	 * guaranteed to contain no null players.
	 * 
	 * @return the players
	 */
	public Set<Player> getPlayers(){
		Set<Player> set = new HashSet<Player>();
		for(Player each : Bukkit.getOnlinePlayers())
			if(each != null && getState(each) == PlayerState.PLAYING)
				set.add(each);
		return set;
	}
	
	/**
	 * Runs through the votes map and gets the option that has recieved the most
	 * votes.
	 * 
	 * @return the most voted-for option
	 */
	public String getMostVoted(){
		StringMap<Integer> tally = new StringMap<Integer>(0);
		for(String vote : votes.values())
			tally.put(vote, tally.get(vote) + 1);
		String mostVoted = null;
		int mostVotes = 0;
		for(String vote : tally.keySet())
			if(tally.get(vote) > mostVotes){
				mostVoted = vote;
				mostVotes = tally.get(vote);
			}
		votes.clear();
		return mostVoted;
	}
	
	/**
	 * Gets the lobby.
	 * 
	 * @return the lobby
	 */
	public Location getLobby(){
		return lobby;
	}
	
	/**
	 * Gets the fireworks launch site.
	 * 
	 * @return the fireworks launch site
	 */
	public Location getFireworks(){
		return fireworks;
	}
	
	/**
	 * Gets the welcome spawn point.
	 * 
	 * @return the welcome spawn point
	 */
	public Location getWelcome(){
		return welcome;
	}
	
	protected void setLobby(Location lobby){
		this.lobby = lobby;
	}
	
	protected void setFireworks(Location fireworks){
		this.fireworks = fireworks;
	}
	
	protected void setWelcome(Location welcome){
		this.welcome = welcome;
	}

	
    
	/**
     * Gets the time the game started.
     * 
     * @return the time
     */
    public long getGameStart(){
    	return gameStart;
    }
    
	

    protected void stampGameStart(){
    	gameStart = System.currentTimeMillis();
    }
	
}
