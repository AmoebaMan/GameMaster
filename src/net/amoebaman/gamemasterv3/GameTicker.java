package net.amoebaman.gamemasterv3;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.amoebaman.gamemasterv3.api.TeamAutoGame;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.modules.SafeSpawnModule;
import net.amoebaman.gamemasterv3.modules.TimerModule;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;
import net.amoebaman.utils.nms.StatusBar;

public class GameTicker implements Runnable{
	
	private boolean tickDebug = false, announcedMinute = false;
	private int timelock = -1;
	
	private GameMaster master;
	
	protected GameTicker(GameMaster master){
		this.master = master;
	}
	
	protected void debug(){
		tickDebug = true;
	}
	
	public boolean isDebugging(){
		return tickDebug;
	}
	
	public void run(){
		if(tickDebug)
			master.log("Printing debug information for one tick...");
		/*
		 * Enforce the time lock
		 */
		if(timelock > 0){
			World world = master.getState() == GameState.INTERMISSION ? master.getLobby().getWorld() : master.getActiveMap().getProperties().getWorld("world");
			world.setTime(timelock);
		}
		/*
		 * If the game is running...
		 */
		if(master.getState() == GameState.RUNNING){
			/*
			 * Team game balancing
			 */
			if(master.getActiveGame() instanceof TeamAutoGame)
				((TeamAutoGame) master.getActiveGame()).balanceTeams();
			/*
			 * Safe spawn protection
			 */
			if(master.getActiveGame() instanceof SafeSpawnModule){
				SafeSpawnModule game = (SafeSpawnModule) master.getActiveGame();
				
				for(Player p : master.getPlayers())
					if(game.getSafeLoc(p) != null){
						Location loc = p.getLocation();
						if(loc.distance(game.getSafeLoc(p)) < game.getSafeRadius(p)){
							long timeDiff = System.currentTimeMillis() - master.getPlayerManager().getLastDamageTime(p);
							if(timeDiff < 1000 * game.getSafeReentryTimeout(p)){
								p.damage(1);
								p.setVelocity(loc.toVector().subtract(game.getSafeLoc(p).toVector()).multiply(0.1));
								new Message(Scheme.WARNING)
								.t("You can't re-enter spawn for ")
								.t(game.getSafeReentryTimeout(p) - timeDiff / 1000).s()
								.t(" more seconds");
							}
							else
								p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 35, 6), true);
						}
					}
			}
			/*
			 * Timer ops
			 */
			if(master.getActiveGame() instanceof TimerModule){
				TimerModule game = (TimerModule) master.getActiveGame();
				
				long millis = game.getGameLength() * 60 * 1000 - (System.currentTimeMillis() - master.getGameStart());
				int seconds = Math.round(millis / 1000F);
				int mins = seconds / 60;
				String status = new Message(Scheme.HIGHLIGHT)
					.t(master.getActiveGame()).s()
					.t(" on ")
					.t(master.getActiveMap()).s()
					.t(" - ")
					.t(mins + ":" + (seconds % 60 < 10 ? "0" + (seconds % 60) : seconds % 60)).s()
					.toString();
				StatusBar.setAllStatusBars(status, 1.0f * seconds / (game.getGameLength() * 60), 1);
				
				if(millis <= 1000)
					game.end();
				else if(seconds % 60 == 0 && mins > 0){
					if(!announcedMinute){
						Chat.broadcast(new Message(Scheme.HIGHLIGHT).then(mins + " minutes").strong().then(" remain on the clock"));
						announcedMinute = true;
					}}
				else
					announcedMinute = false;
			}
		}
		else
			if(master.getState() == GameState.INTERMISSION){
				Message status = new Message(Scheme.HIGHLIGHT)
					.t("Intermission ").s()
					.t(" - waiting to play ");
				if(master.getActiveGame() != null){
					status.t(master.getActiveGame()).s();
					if(master.getActiveMap() != null)
						status.t(" on ").t(master.getActiveMap()).s();
				}
				else
					status.t("the next game");
				
				StatusBar.setAllStatusBars(status.toString(), (System.currentTimeMillis() - master.getGameStart()) / (master.getProgression().getIntermissionLength() * 1000f), 1);
			}		
		else
			StatusBar.removeAllStatusBars();
		/*
		 * Update player names
		 */
		if(tickDebug)
			master.log("Updating player colors");
		for(Player player : master.getPlayers())
			master.getPlayerManager().updateColors(player);
		/*
		 * Kick AFK suckers
		 */
		for(Player player : master.getPlayers())
			; //TODO implement
		/*
		 * Turn off the debug cycle
		 */
		if(tickDebug)
			master.log("Debug is finished");
		tickDebug = false;
	}

}
