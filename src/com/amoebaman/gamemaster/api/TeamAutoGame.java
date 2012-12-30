package com.amoebaman.gamemaster.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.amoebaman.gamemaster.GameMaster;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.enums.PlayerStatus;
import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.ChatUtils;
import com.amoebaman.gamemaster.utils.ListUtils;
import com.amoebaman.gamemaster.utils.Utils;

public abstract class TeamAutoGame extends AutoGame implements Listener{
	
	public static boolean balanceTeams = true;
	
	public String pointName = "points";
	public HashMap<Team, UniqueList<Player>> teams;
	public HashMap<Team, Integer> scores;

	public void init(int respawnTime, String... aliases){
		super.init(respawnTime, aliases);
		teams = new HashMap<Team, UniqueList<Player>>();
		scores = new HashMap<Team, Integer>();
		
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){ public void run(){
			if(isActive() && balanceTeams)
				balanceTeams();
		}}, 10L, 10L);
	}
	
	public void init(String pointName, int respawnTime, String... aliases){
		init(respawnTime, aliases);
		this.pointName = pointName;
	}

	@EventHandler
	public void regulateDamage(final EntityDamageEvent event){
		/*
		 * Get the actual player involved
		 * We consider wolves being attacked to be the player they belong to
		 * Evaluate them by their owner's circumstance
		 */
		Player victim = null;
		if(event.getEntity() instanceof Player)
			victim = (Player) event.getEntity();
		if(event.getEntity() instanceof Wolf && ((Wolf) event.getEntity()).getOwner() instanceof Player)
			victim = (Player) ((Wolf) event.getEntity()).getOwner();
		if(victim == null)
			return;
		/*
		 * If it's an actual PvP event, and the plugin and player are active...
		 */
		if(event instanceof EntityDamageByEntityEvent && isActive() && GameMaster.players.get(victim) == PlayerStatus.PLAYING){
			/*
			 * Get the player who dealt the damage
			 * Again, evaluate pets through their owners
			 */
			EntityDamageByEntityEvent eEvent = (EntityDamageByEntityEvent) event;
			Player damager = null;
			if(eEvent.getDamager() instanceof Player)
				damager = (Player) eEvent.getDamager();
			if(eEvent.getDamager() instanceof Projectile){
				Projectile proj = (Projectile) eEvent.getDamager();
				if(proj.getShooter() instanceof Player)
					damager = (Player) proj.getShooter();
			}
			if(eEvent.getDamager() instanceof Wolf)
				damager = (Player) ((Wolf) eEvent.getDamager()).getOwner();
			if(damager == null) return;
			/*
			 * Teammates can't damage each other
			 */
			if(getTeam(victim) == getTeam(damager)){
				event.setCancelled(true);
				return;
			}
			/*
			 * No punching enemies in their spawn
			 */
			if(Utils.spawnDistance(victim) < 7){
				event.setCancelled(true);
				damager.sendMessage(ChatUtils.ERROR + "You can't damage enemies that are in their spawn");
			}
			/*
			 * No shooting from spawn safety
			 */
			if(Utils.spawnDistance(damager) < 7 && Utils.distance(victim, GameMaster.activeGame.getRespawnLoc(damager)) >= 7){
				event.setCancelled(true);
				damager.sendMessage(ChatUtils.ERROR + "You can't damage enemies out of your spawn");
			}
			/*
			 * Ensure that the player's last damage reflects this occurrence
			 * This is needed to give credit to owners for their wolves' kills
			 */
			final Player finalVictim = victim, finalDamager = damager;
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.getPlugin(), new Runnable(){ public void run(){
				finalVictim.setLastDamageCause(new EntityDamageByEntityEvent(finalVictim, finalDamager, DamageCause.ENTITY_ATTACK, event.getDamage()));
			}});
		}
	}

	public Team getTeam(Player player){
		if(player == null)
			return null;
		for(Team color : teams.keySet())
			if(teams.get(color).contains(player))
				return color;
		return null;
	}
	
	public ChatColor getChatColor(Player player){
		Team team = getTeam(player);
		return team == null ? ChatColor.MAGIC : team.chat;
	}
	
	public final void extractPlayer(Player player){
		extractTeamPlayer(player);
		for(Team teamColor : teams.keySet())
			teams.get(teamColor).remove(player);
	}
	@Deprecated
	public abstract void extractTeamPlayer(Player player);

	public void swapTeam(Player player) {
		List<Team> activeTeams = getActiveMap().getActiveTeams();
		Team team = getTeam(player);
		int teamIndex = activeTeams.indexOf(team) + 1;
		
		player.sendMessage(ChatUtils.HIGHLIGHT + "Your team is being swapped");
		Utils.clearInventory(player);
		player.setHealth(0);
		extractPlayer(player);
		
		teams.get(activeTeams.get(teamIndex >= activeTeams.size() ? 0 : teamIndex)).add(player);
	}

	public Location getRespawnLoc(Player player) {
		return getActiveMap().respawns.get(getTeam(player));
	}

	public void balanceTeams(){
		for(Player player : GameMaster.getPlayers(PlayerStatus.PLAYING))
			if(getTeam(player) == null)
				insertPlayer(player);
		for(Team color : teams.keySet())
			if(getSize(color) > getIdealSize(color) + 2)
				swapTeam(teams.get(color).getRandom());
	}

	public void insertPlayer(Player player){
		double least = Integer.MAX_VALUE;
		Team smallest = null;
		for(Team color : getActiveMap().getActiveTeams())
			if(getSize(color) < least){
				least = getSize(color);
				smallest = color;
			}
		teams.get(smallest).add(player);
		Utils.resetPlayerStatus(player, false);
		player.teleport(getRespawnLoc(player));
		Utils.message(player, getWelcomeMessage(player));
	}

	public double getSize(Team team){
		return teams.get(team).size();
	}

	public int getIdealSize(Team team){
		return GameMaster.getPlayers(PlayerStatus.PLAYING).size() / getActiveMap().getNumTeams();
	}

	/*
	 * Message handling
	 */
	
	@Override
	public final List<String> getWelcomeMessage(Player player){
		List<String> message = new ArrayList<String>();
		message.add(ChatUtils.HIGHLIGHT + "The current game is " + ChatUtils.highlightEmphasis(getGameName()));
		message.add(ChatUtils.HIGHLIGHT + "You are playing on " + ChatUtils.highlightEmphasis(getActiveMap().name));
		Team team = getTeam(player);
		if(team != null)
			message.add(team.chat + "You are on the " + team.toString() + " team");
		message.addAll(getTeamWelcomeMessage(player));
		return message;
	}
	@Deprecated
	public abstract List<String> getTeamWelcomeMessage(Player player);

	@Override
	public final List<String> getSpawnMessage(Player player){
		List<String> message = new ArrayList<String>();
		message.addAll(getTeamSpawnMessage(player));
		return message;
	}
	@Deprecated
	public abstract List<String> getTeamSpawnMessage(Player player);
	
	@Override
	public final List<String> getStatus(Player player){
		List<String> message = new ArrayList<String>();
		message.add(ChatUtils.HIGHLIGHT + "The current game is " + ChatUtils.highlightEmphasis(getGameName()));
		message.add(ChatUtils.HIGHLIGHT + "You are playing on " + ChatUtils.highlightEmphasis(getActiveMap().name));
		Team team = getTeam(player);
		if(team != null)
			message.add(team.chat + "You are on the " + team.toString() + " team");
		message.addAll(getTeamStatus(player));
		return message;
	}
	@Deprecated
	public abstract List<String> getTeamStatus(Player player);
	
	public List<String> simpleStatus(Player player){
		List<String> message = new ArrayList<String>();
		for(Team other : getActiveMap().getActiveTeams())
			message.add(ChatUtils.NORMAL + "Team " + ChatUtils.normalColor(other.chat, other.toString()) + " has " + scores.get(other) + " " + pointName);
		return message;
	}
	
	/*
	 * Game flow handling
	 */
	
	@Override
	public final void prepGame(){
		prepTeamGame();
		teams.clear();
		scores.clear();
		for(Team team : getActiveMap().getActiveTeams()){
			teams.put(team, new UniqueList<Player>());
			scores.put(team, 0);
		}
	}
	@Deprecated
	public abstract void prepTeamGame();
	
	@Override
	public final void startGame(){
		startTeamGame();
	}
	@Deprecated
	public abstract void startTeamGame();
	
	public void simpleStart(){
		Utils.broadcast(
				ChatUtils.DESIGN_RIGHT + ChatUtils.HIGHLIGHT + getGameName().toUpperCase() + " IS STARTING" + ChatUtils.DESIGN_LEFT,
				ChatUtils.NORMAL + "The game will play on " + ChatUtils.normalEmphasis(getActiveMap().name)
				);
		UniqueList<Player> players = ListUtils.sort(GameMaster.getPlayers(PlayerStatus.PLAYING));
		List<UniqueList<Player>> split = ListUtils.split(players, getActiveMap().getNumTeams());
		for(int i = 0; i < getActiveMap().getNumTeams(); i++)
			teams.put(getActiveMap().getActiveTeams().get(i), split.get(i));
		for(Player player : players){
			Utils.resetPlayerStatus(player, false);
			player.teleport(getRespawnLoc(player));
			Utils.message(player, getWelcomeMessage(player));
		}
	}
	
	@Override
	public final void endGame(Object winner){
		if(!(winner instanceof Team)){
			Utils.broadcast(ChatUtils.HIGHLIGHT + getGameName() + " has been ended by an admin");
			GameMaster.enterIntermission();
		}
		else{
			endTeamGame((Team) winner);
		}
	}
	@Deprecated
	public abstract void endTeamGame(Team winner);

	public void simpleEnd(Team winner){
		GameMaster.status = MasterStatus.SUSPENDED;
		Utils.broadcast(
				ChatUtils.DESIGN_RIGHT + ChatUtils.HIGHLIGHT + getGameName().toUpperCase() + " IS FINISHED " + ChatUtils.DESIGN_LEFT,
				(winner == Team.NEUTRAL) ? ChatUtils.NORMAL + "The match ended in a draw" : ChatUtils.NORMAL + "The " + ChatUtils.normalColor(winner.chat, winner.toString()) + " team has won the game"
				);
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){ public void run(){
			GameMaster.enterIntermission();
		} }, 100);
	}
	
	/*
	 * Map handling
	 */
	
	@Override
	public abstract TeamGameMap getActiveMap();
	@Override
	public abstract TeamGameMap getMap(String name);
	@Deprecated
	public abstract UniqueList<TeamGameMap> getTeamMaps();
	@Override
	public final UniqueList<GameMap> getMaps(){ 
		UniqueList<GameMap> maps = new UniqueList<GameMap>();
		maps.addAll(getTeamMaps());
		return maps;
	}

	public static enum Team{
		BLUE(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, true),
		RED(ChatColor.RED, DyeColor.RED, Material.RECORD_3, true),
		GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, Material.GREEN_RECORD, true),
		YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, Material.GOLD_RECORD, true),
		BLACK(ChatColor.BLACK, DyeColor.BLACK, Material.RECORD_8, true),
		WHITE(ChatColor.WHITE, DyeColor.WHITE, Material.RECORD_9, true),
		PURPLE(ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA, Material.RECORD_12, true),
		CYAN(ChatColor.DARK_AQUA, DyeColor.CYAN, Material.RECORD_7, true),
		DEFEND(ChatColor.BLUE, DyeColor.BLUE, Material.RECORD_6, false),
		ATTACK(ChatColor.RED, DyeColor.RED, Material.RECORD_3, false),
		NEUTRAL(ChatColor.GRAY, DyeColor.SILVER, Material.RECORD_11, false);
		;
		
		public final ChatColor chat;
		public final DyeColor dye;
		public final Material disc;
		public final boolean normal;
		private Team(ChatColor chat, DyeColor dye, Material disc, boolean normal){
			this.chat = chat;
			this.dye = dye;
			this.disc = disc;
			this.normal = normal;
		}
		
		public String toString(){
			return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
		}
		
		public static Team getByString(String str){
			for(Team color : values())
				if(color.name().equalsIgnoreCase(str) && color != NEUTRAL)
					return color;
			return null;
		}
		
		public static Team getByChat(ChatColor chat){
			for(Team color : values())
				if(color.chat == chat)
					return color;
			return null;
		}
		
		public static Team getByDye(DyeColor dye){
			for(Team color : values())
				if(color.dye == dye)
					return color;
			return null;
		}
		
		public static Team getByDisc(Material disc){
			if(!disc.isRecord())
				return null;
			for(Team color : values())
				if(color.disc == disc)
					return color;
			return null;
		}
		
		public static boolean isTeam(ChatColor chat){
			return getByChat(chat) != null;
		}
		
		public static boolean isTeam(DyeColor dye){
			return getByDye(dye) != null;
		}
		
		public static boolean isTeam(Material disc){
			return getByDisc(disc) != null;
		}
	}
	
}
