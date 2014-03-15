package net.amoebaman.gamemaster.api;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.amoebaman.gamemaster.GameMaster;
import net.amoebaman.gamemaster.enums.Team;
import net.amoebaman.gamemaster.modules.RespawnModule;
import net.amoebaman.gamemaster.modules.SafeSpawnModule;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;

/**
 * A partially pre-implemented subclass of {@link AutoGame} that handles the
 * basic foundations of a team-based game. It automatically handles things like
 * team membership, balancing, scores, and so-forth.
 * 
 * @author AmoebaMan
 */
public abstract class TeamAutoGame extends AutoGame implements SafeSpawnModule{
	
	/** Contains the scores for each team */
	public static Map<Team, Integer> scores = new HashMap<Team, Integer>();
	
	/**
	 * Whether or not team-balancing is in effect, this can be toggled via
	 * command
	 */
	public static boolean balancing = true;
	
	/**
	 * Gets the {@link Scoreboard} that team games use to display their scores,
	 * the
	 * main Bukkit scoreboard.
	 * 
	 * @return the main scoreboard
	 */
	public static Scoreboard getBoard(){
		return Bukkit.getScoreboardManager().getMainScoreboard();
	}
	
	/**
	 * Team games have a basic requirement that game maps have defined the teams
	 * that play on them, and spawn points for each team. Beyond that, games
	 * that
	 * subclass TeamAutoGame must check for specific compatibility with
	 * {@link #hasCompatibility(GameMap)}.
	 */
	public final boolean isCompatible(GameMap map){
		Set<Team> teams = getActiveTeams(map);
		if(teams.size() < 2)
			return false;
		for(Team team : teams)
			if(map.properties.getLocation("team-respawn/" + team.name()) == null)
				return false;
		return hasCompatibility(map);
	}
	
	/**
	 * Checks to make sure a {@link GameMap} contains all the information that
	 * is
	 * required to run the game on that map.
	 * 
	 * @param map a game map
	 * @return true if the game can run without error on the map, false
	 *         otherwise
	 */
	public abstract boolean hasCompatibility(GameMap map);
	
	public Team getTeam(Player player){
		if(player == null)
			return null;
		org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
		if(team != null)
			return Team.getByString(team.getName());
		else
			return null;
	}
	
	/**
	 * Sets a player's team.
	 * 
	 * @param player a player
	 * @param team a team
	 */
	public void setTeam(Player player, Team team){
		if(getBoard().getPlayerTeam(player) != null)
			getBoard().getPlayerTeam(player).removePlayer(player);
		if(team != null)
			team.getBukkitTeam().addPlayer(player);
	}
	
	/**
	 * Gets all the players on a team.
	 * 
	 * @param team a team
	 * @return all players on the team
	 */
	public Set<Player> getPlayers(Team team){
		Set<Player> players = new HashSet<Player>();
		for(OfflinePlayer player : team.getBukkitTeam().getPlayers())
			if(player.isOnline())
				players.add(player.getPlayer());
		return players;
	}
	
	private void validateScoreboard(){
		Objective obj = getBoard().getObjective("score");
		if(obj == null){
			obj = getBoard().registerNewObjective("score", "dummy");
			obj.setDisplayName(ChatColor.GOLD + "  -=[ Score ]=-  ");
			obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		}
	}
	
	/**
	 * Gets a team's score.
	 * 
	 * @param team a team
	 * @return the team's score
	 */
	public int getScore(Team team){
		validateScoreboard();
		Objective obj = getBoard().getObjective("score");
		if(obj.getScore(Bukkit.getOfflinePlayer(team.chat + team.toString())) != null)
			return obj.getScore(Bukkit.getOfflinePlayer(team.chat + team.toString())).getScore();
		else
			return 0;
	}
	
	/**
	 * Sets a team's score.
	 * 
	 * @param team a team
	 * @param newScore the team's score
	 */
	public void setScore(Team team, int newScore){
		validateScoreboard();
		Objective obj = getBoard().getObjective("score");
		if(newScore < 0)
			getBoard().resetScores(Bukkit.getOfflinePlayer(team.chat + team.toString()));
		else
			obj.getScore(Bukkit.getOfflinePlayer(team.chat + team.toString())).setScore(newScore);
	}
	
	/**
	 * Gets all the teams that have been set to play on a {@link GameMap}.
	 * 
	 * @param map a map
	 * @return the teams that play on the map
	 */
	public Set<Team> getActiveTeams(GameMap map){
		Set<Team> set = new HashSet<Team>();
		List<String> strs = map.properties.getStringList("active-teams");
		if(strs == null)
			return set;
		for(String str : strs){
			Team team = Team.getByString(str);
			if(team != null && team.normal)
				set.add(team);
		}
		return set;
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
		Chat.send(player, new Message(Scheme.NORMAL).then("You've been swapped to the ").then(newTeam).color(newTeam.chat).then(" team"));
		setTeam(player, newTeam);
		player.teleport(getRespawnLoc(player));
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
		return 1f * GameMaster.getPlayers().size() / getActiveTeams(GameMaster.activeMap).size();
	}
	
	/**
	 * Gets a player's respawn location, serving the exact same purpose as
	 * {@link RespawnModule#getRespawnLoc(Player)}.
	 * 
	 * @param player a player
	 * @return the player's respawn location
	 */
	public abstract Location getRespawnLoc(Player player);
	
	/**
	 * Balances teams, swapping players from teams with too many players to
	 * those without enough.
	 */
	public abstract void balanceTeams();
	
	/**
	 * Automatically changes a player to another team, letting the game decide
	 * which team they should go to.
	 * 
	 * @param player a player
	 */
	public abstract void changeTeam(Player player);
	
}
