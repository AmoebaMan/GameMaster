package com.amoebaman.gamemaster.api;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.gamemaster.utils.ChatUtils;


public abstract class FFATimedAutoGame extends FFAAutoGame {

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
	public abstract Player getLeader();
	
	public Player simpleLeader(){
		int maxScore = 0;
		Player winner = null;
		for(Player player : scores.keySet()){
			if(scores.get(player) > maxScore){
				maxScore = scores.get(player);
				winner = player;
			}
		}
		return winner;
	}
	
	@Override
	public final List<String> getFFAStatus(Player player){
		List<String> message = new ArrayList<String>();
		message.addAll(getFFATimedStatus(player));
		message.add(ChatUtils.NORMAL + "There are " + ChatUtils.normalEmphasis(getMinutesRemaining()) + " minutes and " + ChatUtils.normalEmphasis(getSecondsRemaining() % 60) + " seconds remaining");
		return message;
	}
	public abstract List<String> getFFATimedStatus(Player player);

	public final void startFFAGame(){
		gameStart = System.currentTimeMillis();
		startFFATimedGame();
	}
	public abstract void startFFATimedGame();
	
	public abstract FFATimedGameMap getActiveMap();
	public abstract FFATimedGameMap getMap(String name);
	public abstract UniqueList<FFATimedGameMap> getFFATimedMaps();
	public final UniqueList<FFAGameMap> getFFAMaps(){ 
		UniqueList<FFAGameMap> maps = new UniqueList<FFAGameMap>();
		maps.addAll(getFFATimedMaps());
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
