package net.amoebaman.gamemasterv3;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criterias;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import com.dsh105.holoapi.HoloAPI;
import com.dsh105.holoapi.api.Hologram;
import com.dsh105.holoapi.api.HologramFactory;

import net.amoebaman.gamemasterv3.api.AutoGame;
import net.amoebaman.gamemasterv3.api.GameMap;
import net.amoebaman.gamemasterv3.api.TeamAutoGame;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.gamemasterv3.enums.Team;
import net.amoebaman.gamemasterv3.modules.TimerModule;
import net.amoebaman.gamemasterv3.util.Utils;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.statmaster.Statistic;
import net.amoebaman.utils.CommandController;
import net.amoebaman.utils.S_Loc;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;
import net.amoebaman.utils.maps.PlayerMap;
import net.amoebaman.utils.maps.StringMap;
import net.amoebaman.utils.nms.StatusBar;

import net.minecraft.util.com.google.common.collect.Lists;

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
	
	private PlayerMap<Hologram> statusHolos = new PlayerMap<Hologram>();
	private ItemStack holoHoldItem;
	
	private AutoGame activeGame;
	private GameMap activeMap, editMap;
	
	private Location lobby, fireworks, welcome;
	
	private int tickTaskId = 0;
	
	private GameState state = GameState.INTERMISSION;
	private long gameStart = 0L;
	
	private CommandListener commands;
	private EventListener events;
	private GameTicker ticker;
	private Progression progression;
	private Players playerManager;
	
	public void onEnable(){
		INSTANCE = this;
		/*
		 * Set up components
		 */
		commands = new CommandListener(this);
		events = new EventListener(this);
		CommandController.registerCommands(commands);
		Bukkit.getPluginManager().registerEvents(events, this);
		progression = new Progression(this);
		playerManager = new Players(this);
		/*
		 * Establish files and folders
		 */
		configFile = new File(getDataFolder().getPath() + "/config.yml");
		mapsFile = new File(getDataFolder().getPath() + "/maps.yml");
		repairFile = new File(getDataFolder().getPath() + "/repair.yml");
		/*
		 * Load configs
		 */
		try{
			/*
			 * The "proper" configuration
			 */
			getConfig().load(configFile);
			getConfig().options().copyDefaults(true);
			getConfig().options().copyHeader(true);
			getConfig().save(configFile);
			
			lobby = S_Loc.stringLoad(getConfig().getString("locations.lobby"));
			if(lobby == null)
				lobby = new Location(Bukkit.getWorlds().get(0), 0.5, 80, 0.5);
			fireworks = S_Loc.stringLoad(getConfig().getString("locations.fireworks"));
			if(fireworks == null)
				fireworks = lobby.clone();
			welcome = S_Loc.stringLoad(getConfig().getString("locations.welcome"));
			if(welcome == null)
				welcome = lobby.clone();
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
					if(!sec.isConfigurationSection(prop))
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
		 * Set up hologram stuff
		 */
		holoHoldItem = new ItemStack(Material.PAPER);
		ItemMeta meta = holoHoldItem.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "Game Status");
		meta.setLore(Lists.newArrayList(ChatColor.GOLD + "Hold to view game status"));
		holoHoldItem.setItemMeta(meta);
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){ public void run(){
			for(Player player : Bukkit.getOnlinePlayers())
				moveStatusHolo(player);
		}}, 0L, 1L);
		/*
		 * Start the ticker
		 */
		ticker = new GameTicker(this);
		tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ticker, 0, 5L);
		/*
		 * Do scoreboard stuff
		 */
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective health = board.getObjective("health");
		if(health == null){
			health = board.registerNewObjective("health", Criterias.HEALTH);
			health.setDisplaySlot(DisplaySlot.BELOW_NAME);
			health.setDisplayName("HP");
		}
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
		 * Remove all funky packet things
		 */
		StatusBar.removeAllStatusBars();
		for(Player player : Bukkit.getOnlinePlayers())
			removeStatusHolo(player);
		/*
		 * Save the configs
		 */
		try{
			getConfig().set("locations/lobby", S_Loc.stringSave(lobby, true, true));
			getConfig().set("locations/fireworks", S_Loc.stringSave(fireworks, true, false));
			getConfig().set("locations/welcome", S_Loc.stringSave(welcome, true, true));
			getConfig().save(configFile);
			
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
	
	protected Collection<AutoGame> getGames(){
		return new HashSet(games.values());
	}
	
	protected Collection<GameMap> getMaps(){
		return new HashSet(maps.values());
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
			for(String alias : game.getAliases())
				games.put(alias, game);
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
			Location loc = S_Loc.stringLoad(key);
			if(loc != null){
				Block block = S_Loc.stringLoad(key).getBlock();
				String[] split = repairYaml.getString(key).split(":");
				if(block.getType() == Material.CHEST)
					((Chest) block.getState()).getBlockInventory().clear();
				block.setTypeIdAndData(Integer.parseInt(split[0]), Byte.parseByte(split[1]), false);
			}
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
	
	protected EventListener getListener(){
		return events;
	}
	
	/**
	 * Gets the ticker.
	 * 
	 * @return the ticker
	 */
	public GameTicker getTicker(){
		return ticker;
	}
	
	protected Progression getProgression(){
		return progression;
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
	 * Pauses the game.
	 */
	public void pauseGame(){
		setState(GameState.PAUSED);
	}
	
	/**
	 * Resumes the game.
	 */
	public void resumeGame(){
		setState(GameState.RUNNING);
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
		if(state != GameState.INTERMISSION){
			if(newState == PlayerState.PLAYING){
				playerManager.resetPlayer(player);
				activeGame.join(player);
			}
			else
				activeGame.leave(player);
		}
		else
			if(newState != PlayerState.EXTERIOR)
				player.teleport(lobby);
		/*
		 * Update game modes
		 */
		if(newState == PlayerState.PLAYING)
			player.setGameMode(GameMode.SURVIVAL);
		if(newState == PlayerState.WATCHING){
			player.setGameMode(GameMode.CREATIVE);
			player.getInventory().addItem(getHoloItem());
		}
		/*
		 * Update visibilities
		 */
		for(Player other : Bukkit.getOnlinePlayers())
			if(!other.equals(player)){
				if(getState(other) == PlayerState.PLAYING && newState == PlayerState.WATCHING)
					other.hidePlayer(player);
				else
					other.showPlayer(player);
				if(newState == PlayerState.PLAYING && getState(other) == PlayerState.WATCHING)
					player.hidePlayer(other);
				else
					player.showPlayer(other);
			}
		/*
		 * Update colors
		 */
		playerManager.updateColors(player);
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
	
	/**
	 * Gets the map being edited.
	 * 
	 * @return the map
	 */
	public GameMap getEditMap(){
		return editMap;
	}
	
	protected void setEditMap(GameMap map){
		editMap = map;
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
	
	public Set<Player> getWatchers(){
		Set<Player> set = new HashSet<Player>();
		for(Player each : Bukkit.getOnlinePlayers())
			if(each != null && getState(each) == PlayerState.WATCHING)
				set.add(each);
		return set;
	}
	
	/**
	 * Lots a player's vote.
	 * 
	 * @param player a player
	 * @param vote their vote
	 */
	public void logVote(Player player, String vote){
		votes.put(player, vote);
	}
	
	/**
	 * Runs through the votes map and gets the option that has recieved the most
	 * votes, and clears all votes.
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
	
	/**
	 * Logs a message to the console.
	 * 
	 * @param message a message
	 */
	public void log(String message){
		getLogger().info(message);
	}
	
	/**
	 * Ends the game and begins the intermission.
	 */
	public void endGame(){
		progression.intermission();
	}
	
	protected void sendStatusHolo(CommandSender sender){
		
		final Player player = sender instanceof Player ? (Player) sender : null;
		
		List<String> status = Lists.newArrayList();
		
		if(getState() != GameState.INTERMISSION){
			status.add(
				new Message(Scheme.HIGHLIGHT)
				.t("Playing ")
				.t(getActiveGame()).s()
				.t(" on ")
				.t(getActiveMap()).s()
				.getText()
			);
			if(getActiveGame() instanceof TeamAutoGame && player != null && getState(player) == PlayerState.PLAYING){
				Team team = ((TeamAutoGame) getActiveGame()).getTeam(player);
				status.add(
					new Message(Scheme.HIGHLIGHT)
					.t("You're on the ")
					.t(team).color(team.chat)
					.t(" team")
					.getText()
					);
			}
			for(Object msg : getActiveGame().getStatusMessages(player))
				if(msg instanceof Message)
					status.add(((Message) msg).getText());
				else
					status.add(String.valueOf(msg));
			if(getActiveGame() instanceof TimerModule){
				long millis = ((TimerModule) getActiveGame()).getGameLength() * 60 * 1000 - (System.currentTimeMillis() - getGameStart());
				int seconds = Math.round(millis / 1000F);
				int mins = seconds / 60;
				status.add(
					new Message(Scheme.NORMAL)
					.t(mins).s()
					.t(" minutes and ")
					.t(seconds % 60).s()
					.t(" seconds remain")
					.getText()
					);
			}
		}
		else
			status.add(
				new Message(Scheme.WARNING)
				.t("No game is playing right now")
				.getText()
			);
			
		
		if(player == null)
			Chat.send(sender, status);
		else{
			removeStatusHolo(player);
			Hologram holo = new HologramFactory(this)
			.withLocation(Utils.getHoloHudLoc(player).add(new Vector(0, 0.25 * status.size(), 0)))
			.withText(status.toArray(new String[0]))
			.build();
			holo.clearAllPlayerViews();
			holo.show(player);
			statusHolos.put(player, holo);
		}
	}
	
	protected void moveStatusHolo(Player player){
		if(statusHolos.containsKey(player))
			statusHolos.get(player).move(player, Utils.getHoloHudLoc(player).toVector().add(new Vector(0, 0.25 * statusHolos.get(player).getLines().length, 0)));
	}
	
	protected void removeStatusHolo(Player player){
		if(statusHolos.containsKey(player)){
			Hologram holo = statusHolos.get(player);
			holo.clearAllPlayerViews();
			HoloAPI.getManager().stopTracking(holo);
			HoloAPI.getManager().clearFromFile(holo.getSaveId());
			statusHolos.remove(player);
		}
	}
	
	protected ItemStack getHoloItem(){
		return holoHoldItem.clone();
	}
	
}
