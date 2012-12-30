package com.amoebaman.gamemaster.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.amoebaman.gamemaster.GameMaster;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.enums.PlayerStatus;
import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.ChatUtils;
import com.amoebaman.gamemaster.utils.Utils;

public abstract class FFAAutoGame extends AutoGame implements Listener{

	public String pointName = "points";
	public HashMap<Player, Integer> scores;

	public void init(int respawnTime, String... aliases){
		super.init(respawnTime, aliases);
		scores = new HashMap<Player, Integer>();
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	public void init(String pointName, int respawnTime, String... aliases){
		init(respawnTime, aliases);
		this.pointName = pointName;
	}

	public Location getRespawnLoc(Player player) {
		return getActiveMap().getFreeSpawns().getRandom();
	}

	public void insertPlayer(Player player){
		insertFFAPlayer(player);
		if(!scores.containsKey(player))
			scores.put(player, 0);
		Utils.resetPlayerStatus(player, false);
		player.teleport(getRespawnLoc(player));
		Utils.message(player, getWelcomeMessage(player));
	}
	public abstract void insertFFAPlayer(Player player);

	public final void extractPlayer(Player player){
		extractFFAPlayer(player);
		scores.remove(player);
	}
	public abstract void extractFFAPlayer(Player player);

	public ChatColor getChatColor(Player player){ return ChatColor.GRAY; }

	public Player getLeader(){
		int highest = 0;
		Player leader = null;
		for(Player player : scores.keySet())
			if(scores.get(player) > highest){
				highest = scores.get(player);
				leader = player;
			}
		return leader;
	}

	/*
	 * Message handling
	 */

	@Override
	public final List<String> getWelcomeMessage(Player player){
		List<String> message = new ArrayList<String>();
		message.add(ChatUtils.HIGHLIGHT + "The current game is " + ChatUtils.highlightEmphasis(getGameName()));
		message.add(ChatUtils.HIGHLIGHT + "You are playing on " + ChatUtils.highlightEmphasis(getActiveMap().name));
		message.add(ChatUtils.HIGHLIGHT + "Use " + ChatUtils.highlightEmphasis("/kitlist") + " to see available kits");
		message.add(ChatUtils.HIGHLIGHT + "Use " + ChatUtils.highlightEmphasis("/kit <name>") + " to take a kit");
		message.addAll(getFFAWelcomeMessage(player));
		return message;
	}
	@Deprecated
	public abstract List<String> getFFAWelcomeMessage(Player player);

	@Override
	public final List<String> getSpawnMessage(Player player){
		List<String> message = new ArrayList<String>();
		message.add(ChatUtils.HIGHLIGHT + "Use " + ChatUtils.highlightEmphasis("/kitlist") + " to see available kits");
		message.add(ChatUtils.HIGHLIGHT + "Use " + ChatUtils.highlightEmphasis("/kit <name>") + " to take a kit");
		message.addAll(getFFASpawnMessage(player));
		return message;
	}
	@Deprecated
	public abstract List<String> getFFASpawnMessage(Player player);

	@Override
	public List<String> getStatus(Player context){
		List<String> status = new ArrayList<String>();
		status.add(ChatUtils.HIGHLIGHT + "The current game is " + ChatUtils.highlightEmphasis(getGameName()));
		status.add(ChatUtils.HIGHLIGHT + "You are playing on " + ChatUtils.highlightEmphasis(getActiveMap().name));
		status.addAll(getFFAStatus(context));
		return status;
	}
	@Deprecated
	public abstract List<String> getFFAStatus(Player context);

	public List<String> simpleStatus(Player context){
		List<String> status = new ArrayList<String>();
		if(getLeader() == null)
			status.add(ChatUtils.NORMAL + "Nobody has taken the lead yet");
		else
			status.add(ChatUtils.NORMAL + ChatUtils.normalEmphasis(getLeader().getName()) + " is in the lead with " + ChatUtils.normalEmphasis(scores.get(getLeader())) + " " + pointName);
		if(context != null){
			if(!scores.containsKey(context))
				scores.put(context, 0);
			status.add(ChatUtils.NORMAL + "You have " + ChatUtils.normalEmphasis(scores.get(context)) + " " + pointName);
		}
		return status;
	}

	/*
	 * Game flow handling
	 */

	public final void prepGame(){
		prepFFAGame();
		scores.clear();
	}
	@Deprecated
	public abstract void prepFFAGame();

	public final void startGame(){
		startFFAGame();
	}
	@Deprecated
	public abstract void startFFAGame();

	public void simpleStart(){
		Utils.broadcast(
				ChatUtils.DESIGN_RIGHT + ChatUtils.HIGHLIGHT + getGameName().toUpperCase() + " DEATHMATCH IS STARTING " + ChatUtils.DESIGN_LEFT,
				ChatUtils.NORMAL + "The game will play on " + ChatUtils.normalEmphasis(getActiveMap().name)
				);
		for(Player player : GameMaster.getPlayers(PlayerStatus.PLAYING)){
			Utils.resetPlayerStatus(player, false);
			player.teleport(getRespawnLoc(player));
			Utils.message(player, getWelcomeMessage(player));
		}
	}

	public final void endGame(Object winner){
		if(!(winner instanceof Player)){
			Utils.broadcast(ChatUtils.HIGHLIGHT + getGameName() + " has been ended by an admin" );
			GameMaster.enterIntermission();
		}
		else
			endFFAGame((Player) winner);
	}
	@Deprecated
	public abstract void endFFAGame(Player winner);

	public void simpleEnd(Player winner){
		GameMaster.status = MasterStatus.SUSPENDED;
		Utils.broadcast(
				ChatUtils.DESIGN_RIGHT + ChatUtils.HIGHLIGHT + getGameName().toUpperCase() + " IS FINISHED" + ChatUtils.DESIGN_LEFT,
				ChatUtils.normalEmphasis(winner.getName()) + " has won the game with " + scores.get(winner) + " " + pointName
				);
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){ public void run(){
			GameMaster.enterIntermission();
		} }, 100);
	}

	/*
	 * Map handling
	 */

	public abstract FFAGameMap getActiveMap();
	public abstract FFAGameMap getMap(String name);
	public final UniqueList<GameMap> getMaps(){ 
		UniqueList<GameMap> maps = new UniqueList<GameMap>();
		maps.addAll(getFFAMaps());
		return maps;
	}
	public abstract UniqueList<FFAGameMap> getFFAMaps();

}
