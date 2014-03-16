package net.amoebaman.gamemasterv3.api;

import java.util.*;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.kitteh.tag.TagAPI;

import net.amoebaman.gamemasterv3.enums.Team;
import net.amoebaman.gamemasterv3.util.Utils;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.chat.*;
import net.amoebaman.utils.maps.DefaultedMap;
import net.amoebaman.utils.maps.PlayerMap;

/**
 * A partially pre-implemented subclass of {@link AutoGame} that handles the
 * basic foundations of a team-based game. It automatically handles things like
 * team membership, balancing, scores, and so-forth.
 * 
 * @author AmoebaMan
 */
public abstract class TeamAutoGame extends AutoGame{
	
	private static boolean balance = true;
	
	/**
	 * Gets whether or not team balancing is currently enabled.
	 * 
	 * @return whether team balancing is on
	 */
	public static boolean isBalancing(){
		return balance;
	}
	
	/**
	 * Turns team balancing on or off.
	 * 
	 * @param whether team balancing should be on
	 */
	public static void setBalancing(boolean balance){
		TeamAutoGame.balance = balance;
	}
	
	private DefaultedMap<Team, Integer> scores = new DefaultedMap<Team, Integer>(0);
	private PlayerMap<Team> playerTeams = new PlayerMap<Team>();
	private DefaultedMap<Team, Set<Player>> teamPlayers = new DefaultedMap<Team, Set<Player>>();
	
	/**
	 * Team games have a basic requirement that game maps have defined the teams
	 * that play on them, and spawn points for each team. Beyond that, games
	 * that subclass TeamAutoGame should override this method with to add their
	 * own criteria.
	 * <p>
	 * For example, the first line of any ovveriding implementation ought to be:
	 * <p>
	 * <code>if(!super.isCompatible(map)) return false;</code>
	 * 
	 * @param map a map
	 */
	public boolean isCompatible(GameMap map){
		Set<Team> teams = getActiveTeams(map);
		if(teams.size() < 2)
			return false;
		for(Team team : teams)
			if(map.getProperties().getLocation("team-respawn/" + team.name()) == null)
				return false;
		return true;
	}
	
	/**
	 * Gets all the teams that have been set to play on a {@link GameMap}.
	 * 
	 * @param map a map
	 * @return the teams that play on the map
	 */
	public Set<Team> getActiveTeams(GameMap map){
		Set<Team> set = new HashSet<Team>();
		List<String> strs = map.getProperties().getStringList("active-teams");
		if(strs == null)
			return set;
		for(String str : strs){
			Team team = Team.getByString(str);
			if(team != null && team.normal)
				set.add(team);
		}
		return set;
	}
	
	public Team getTeam(Player player){
		return playerTeams.get(player);
	}
	
	/**
	 * Sets a player's team.
	 * 
	 * @param player a player
	 * @param team a team
	 */
	public void setTeam(Player player, Team team){
		if(teamPlayers.containsKey(getTeam(player)))
			teamPlayers.get(getTeam(player).name()).remove(player);
		if(team == null)
			playerTeams.remove(player);
		else{
			playerTeams.put(player, team);
			teamPlayers.get(team.name()).add(player);
		}
	}
	
	/**
	 * Gets all the players on a team.
	 * 
	 * @param team a team
	 * @return all players on the team
	 */
	public Set<Player> getPlayers(Team team){
		if(!teamPlayers.containsKey(team))
			teamPlayers.put(team, new LinkedHashSet<Player>());
		return new HashSet(teamPlayers.get(team.name()));
	}
	
	/**
	 * Gets a team's score.
	 * 
	 * @param team a team
	 * @return the team's score
	 */
	public int getScore(Team team){
		return scores.get(team);
	}
	
	/**
	 * Sets a team's score.
	 * 
	 * @param team a team
	 * @param newScore the team's score
	 */
	public void setScore(Team team, int newScore){
		scores.put(team, newScore);
	}
	
	/**
	 * Switches a player to a new team.
	 * 
	 * @param player a player
	 * @param newTeam a team
	 */
	public void swapTeam(Player player, Team newTeam){
		Team oldTeam = getTeam(player);
		if(newTeam == oldTeam)
			return;
		new Message(Scheme.NORMAL).t("You've been swapped to the ").t(newTeam).color(newTeam.chat).t(" team");
		setTeam(player, newTeam);
		player.teleport(getRespawnLoc(player));
		master.getPlayerManager().destamp(player);
		TagAPI.refreshPlayer(player);
	}
	
	/**
	 * Gets the size of a team. Overriding this method can be done to adjust
	 * team balancing, although overriding {@link #getProperSize(Team)} is
	 * preferred.
	 * 
	 * @param team a team
	 * @return the amount of players on the team
	 */
	public double getSize(Team team){
		return getPlayers(team).size();
	}
	
	/**
	 * Gets the ideal size of a team, considering the number of players
	 * currently playing. Override this method to adjust team balancing.
	 * 
	 * @param team a team
	 * @return the amount of players the team ought to have
	 */
	public double getProperSize(Team team){
		return 1f * master.getPlayers().size() / getActiveTeams(master.getActiveMap()).size();
	}
	
	/**
	 * Gets a player's respawn location.
	 * 
	 * @param player a player
	 * @return the player's respawn location
	 */
	public Location getRespawnLoc(Player player){
		Team team = getTeam(player);
		if(team == null)
			return null;
		return master.getActiveMap().getProperties().getLocation("team-respawn/" + team.name());
	}
	
	/**
	 * Balances teams, swapping players from teams with too many players to
	 * those without enough.
	 */
	public void balanceTeams(){
		/*
		 * If somebody's somehow got a null team, join them in
		 */
		for(Player player : master.getPlayers())
			if(getTeam(player) == null)
				join(player);
		/*
		 * Balance the teams
		 */
		if(balance)
			for(Team team : getActiveTeams(master.getActiveMap())){
				if(master.getTicker().isDebugging())
					master.log(team + "'s size is " + getSize(team) + ", ideal is " + getProperSize(team));
				if(getSize(team) > getProperSize(team) + 1){
					if(master.getTicker().isDebugging())
						master.log(team + " is oversized");
					/*
					 * Move a random player
					 */
					changeTeam(GenUtil.getRandomElement(getPlayers(team)));
				}
			}
	}
	
	/**
	 * Automatically changes a player to another team, letting the game decide
	 * which team they should go to.
	 * 
	 * @param player a player
	 */
	public void changeTeam(Player player){
		/*
		 * Determine which team has the fewest players
		 */
		Team mostNeedy = null;
		double leastPlayers = Bukkit.getMaxPlayers();
		for(Team other : getActiveTeams(master.getActiveMap()))
			if(other != getTeam(player) && getSize(other) < leastPlayers){
				mostNeedy = other;
				leastPlayers = getSize(mostNeedy);
			}
		if(mostNeedy == null)
			mostNeedy = GenUtil.getRandomElement(getActiveTeams(master.getActiveMap()));
		/*
		 * Swap 'em
		 */
		swapTeam(player, mostNeedy);
		if(master.getTicker().isDebugging())
			master.log("Swapped " + player.getName() + " to " + mostNeedy);
	}
	
	public ChatColor getColor(Player player){
		if(!playerTeams.containsKey(player))
			return null;
		return getTeam(player).chat;
	}
	
	public List<Object> getStatusMessages(Player player){
		List<Object> msgs = new ArrayList<Object>();
		for(Team team : scores.keySet())
			msgs.add(new Message(Scheme.NORMAL).t("The ").t(team).color(team.chat).t(" team has ").t(scores.get(team)).s().t(" points"));
		return msgs;
	}
	
	public void join(Player player){
		if(getTeam(player) != null)
			return;
		double biggestDiff = 0;
		Team neediest = null;
		for(Team team : getActiveTeams(master.getActiveMap()))
			if(getProperSize(team) - getSize(team) > biggestDiff){
				biggestDiff = getProperSize(team) - getSize(team);
				neediest = team;
			}
		if(neediest == null)
			return;
		setTeam(player, neediest);
		player.teleport(getRespawnLoc(player));
		master.getPlayerManager().resetPlayer(player);
	}
	
	public void leave(Player player){
		setTeam(player, null);
	}
	
	public void start(){
		/*
		 * Initialize teams
		 */
		Set<Team> activeTeams = getActiveTeams(master.getActiveMap());
		for(Team team : activeTeams)
			team.getBukkitTeam();
		/*
		 * Broadcast
		 */
		Chat.broadcast(Align.addSpacers("" + Scheme.HIGHLIGHT.normal.color() + CustomChar.LIGHT_BLOCK, Align.center(new Message(Scheme.HIGHLIGHT).t(getName().toUpperCase()).s().t(" is starting"), new Message(Scheme.HIGHLIGHT).t(master.getActiveMap()).strong().t(" will be the battlefield"))));
		/*
		 * Split up the teams
		 */
		List<Player> players = Utils.sort(master.getPlayers());
		List<Set<Player>> split = Utils.split(players, activeTeams.size());
		for(Team team : activeTeams)
			for(Player player : split.remove(0))
				setTeam(player, team);
		for(Player player : players)
			player.teleport(getRespawnLoc(player));
	}
	
	public void abort(){
		new Message(Scheme.ERROR).t("The game was aborted by an operator").broadcast();
	}
	
	public int getGameLength(){
		return 15;
	}
	
	public void end(){
		/*
		 * Determine who actually won the game
		 */
		Team winner = null;
		int maxScore = 0;
		for(Team team : getActiveTeams(master.getActiveMap())){
			if(getScore(team) > maxScore){
				winner = team;
				maxScore = getScore(team);
			}
			else
				if(getScore(team) == maxScore)
					winner = Team.NEUTRAL;
		}
		/*
		 * Null winners are draws, change that to the neutral team to avoid
		 * obnoxious NPEs
		 */
		if(winner == null)
			winner = Team.NEUTRAL;
		/*
		 * Increment game stats and award charges
		 */
		for(Player player : master.getPlayers())
			if(getTeam(player) == winner){
				StatMaster.getHandler().incrementStat(player, "wins");
				StatMaster.getHandler().adjustStat(player, "charges", 0.5);
				new Message(Scheme.HIGHLIGHT).t("You have earned ").t("0.5 charges").s().tooltip(Chat.format("Total of &z" + StatMaster.getHandler().getStat(player, "charges"), Scheme.NORMAL)).t(" for winning the game").send(player);
			}
			else
				StatMaster.getHandler().incrementStat(player, "losses");
		/*
		 * Shoot of fireworks (just for shits and giggles)
		 */
		if(winner != null){
			final Color color = winner.dye.getFireworkColor();
			final Color[] grayscale = {Color.BLACK, Color.GRAY, Color.SILVER, Color.WHITE};
			for(int i = 0; i < 50; i++)
				Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
					
					public void run(){
						/*
						 * Create a random burst
						 */
						FireworkEffect burst = FireworkEffect.builder().withColor(color.mixColors(grayscale[(int) (Math.random() * grayscale.length)])).withFade(color.mixColors(grayscale[(int) (Math.random() * grayscale.length)])).with(Type.values()[(int) (Math.random() * Type.values().length)]).flicker(Math.random() > 0.5).trail(Math.random() > 0.5).build();
						FireworkMeta meta = (FireworkMeta) new ItemStack(Material.FIREWORK).getItemMeta();
						meta.addEffect(burst);
						/*
						 * Launch it
						 */
						Location loc = master.getFireworks().clone().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5));
						Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
						firework.setFireworkMeta(meta);
					}
				}, (int) (100 + i * 4 + (Math.random() - 0.5) * 4));
		}
		/*
		 * Suspend the game in preparation for the intermission
		 */
		master.pauseGame();
		/*
		 * Broadcast
		 */
		Chat.broadcast(Align.addSpacers("" + Scheme.HIGHLIGHT.normal.color() + CustomChar.LIGHT_BLOCK, Align.center(new Message(Scheme.HIGHLIGHT).t(getName().toUpperCase()).s().t(" is finished"), winner == Team.NEUTRAL ? new Message(Scheme.HIGHLIGHT).t("The match ended in a draw") : new Message(Scheme.HIGHLIGHT).t("The ").t(winner).color(winner.chat).t(" team won the game"))));
		/*
		 * End the game
		 */
		Bukkit.getScheduler().scheduleSyncDelayedTask(master, new Runnable(){
			
			public void run(){
				master.endGame();
			}
		}, 100);
	}
	
	public int getSafeRadius(Player player){
		return 5;
	}
	
	public int getSafeReentryTimeout(Player player){
		return 10;
	}
	
	public Location getSafeLoc(Player player){
		return getRespawnLoc(player);
	}
	
	public int getRespawnDelay(Player player){
		return 10;
	}
	
	public Location getWaitingLoc(Player player){
		return master.getLobby();
	}
	
	public int getRespawnInvuln(Player player){
		return 5;
	}
	
}
