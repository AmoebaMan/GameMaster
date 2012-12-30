package com.amoebaman.gamemaster.api;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.ChatUtils;


public abstract class TeamTimedAutoGame extends TeamAutoGame {

	public long gameStart;

	public void init(String pointName, int respawnTime, String... aliases){
		super.init(pointName, respawnTime, aliases);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){

			boolean announcedMinute = false;

			public void run(){
				if(isActive()){
					if(getMillisRemaining() <= 1000)
						endGame(getLeader());
					else if(getSecondsRemaining() % 60 == 0 && getMinutesRemaining() > 0){
						if(!announcedMinute){
							Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + ChatUtils.highlightEmphasis(getMinutesRemaining() + " minutes") + " remain on the clock");
							announcedMinute = true;
						}
					}
					else
						announcedMinute = false;
				}
			}
			
		}, 0, 5);
	}
	public abstract Team getLeader();
	
	public Team simpleLeader(){
		int maxScore = 0;
		boolean draw = true;
		Team winner = null;
		for(Team team : getActiveMap().getActiveTeams()){
			if(scores.get(team) > maxScore){
				maxScore = scores.get(team);
				draw = false;
				winner = team;
			}
			else if(scores.get(team) == maxScore)
				draw = true;
		}
		return draw || winner == null ? Team.NEUTRAL : winner;
	}
	
	@Override
	public final List<String> getTeamStatus(Player player){
		List<String> message = new ArrayList<String>();
		message.addAll(getTeamTimedStatus(player));
		message.add(ChatUtils.NORMAL + "There are " + ChatUtils.normalEmphasis(getMinutesRemaining()) + " minutes and " + ChatUtils.normalEmphasis(getSecondsRemaining() % 60) + " seconds remaining");
		return message;
	}
	public abstract List<String> getTeamTimedStatus(Player player);
	
	public final void startTeamGame(){
		gameStart = System.currentTimeMillis();
		startTeamTimedGame();
	}
	public abstract void startTeamTimedGame();
	
	public abstract TeamTimedGameMap getActiveMap();
	public abstract TeamTimedGameMap getMap(String name);
	public abstract UniqueList<TeamTimedGameMap> getTeamTimedMaps();
	public final UniqueList<TeamGameMap> getTeamMaps(){ 
		UniqueList<TeamGameMap> maps = new UniqueList<TeamGameMap>();
		maps.addAll(getTeamTimedMaps());
		return maps;
	}

	public final long getMillisRemaining(){
		return getActiveMap().timeLimitMinutes * 60 * 1000 - (System.currentTimeMillis() - gameStart);
	}

	public final int getSecondsRemaining(){
		return Math.round(getMillisRemaining() / 1000F);
	}

	public final int getMinutesRemaining(){
		return getSecondsRemaining() / 60;
	}

}
