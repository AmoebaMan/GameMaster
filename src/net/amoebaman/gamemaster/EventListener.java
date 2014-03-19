package net.amoebaman.gamemaster;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import net.amoebaman.gamemaster.api.AutoGame;
import net.amoebaman.gamemaster.api.TeamAutoGame;
import net.amoebaman.gamemaster.enums.MasterStatus;
import net.amoebaman.gamemaster.enums.PlayerStatus;
import net.amoebaman.gamemaster.modules.MessagerModule;
import net.amoebaman.gamemaster.modules.RespawnModule;
import net.amoebaman.gamemaster.modules.SafeSpawnModule;
import net.amoebaman.kitmaster.enums.GiveKitContext;
import net.amoebaman.kitmaster.utilities.ClearKitsEvent;
import net.amoebaman.kitmaster.utilities.GiveKitEvent;
import net.amoebaman.statmaster.StatMaster;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.chat.Align;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;
import net.amoebaman.utils.maps.PlayerMap;
import net.amoebaman.utils.nms.StatusBar;

import net.minecraft.util.com.google.common.collect.Lists;

@SuppressWarnings("deprecation")
public class EventListener implements Listener {
	
	public void deregisterUnloadedGames(PluginDisableEvent event){
		if(event.getPlugin() instanceof AutoGame)
			GameMaster.deregisterGame((AutoGame) event.getPlugin());
	}
	
	@EventHandler
	public void blockPlace(BlockPlaceEvent event){
		if(GameMaster.getStatus(event.getPlayer()) != PlayerStatus.ADMIN)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void blockBreak(BlockBreakEvent event){
		if(GameMaster.getStatus(event.getPlayer()) != PlayerStatus.ADMIN)
			event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDamageModify(final EntityDamageEvent event){
		/*
		 * Determine the victim (we consider wolves part of their owners)
		 */
		Player victim = null;
		if(event.getEntity() instanceof Player)
			victim = (Player) event.getEntity();
		if(event.getEntity() instanceof Tameable && ((Tameable) event.getEntity()).getOwner() instanceof Player)
			victim = (Player) ((Tameable) event.getEntity()).getOwner();
		if(victim == null)
			return;
		/*
		 * Don't even bother if no event is running
		 */
		if(GameMaster.status != MasterStatus.RUNNING){
			event.setCancelled(true);
			return;
		}
		/*
		 * Cancel if the victim isn't playing, or is respawning
		 */
		if(GameMaster.getStatus(victim) != PlayerStatus.PLAYING || GameMaster.respawning.contains(victim)){
			event.setCancelled(true);
			return;
		}
		/*
		 * We're only really concerned about players damaged by entities here...
		 */
		if(event instanceof EntityDamageByEntityEvent){
			/*
			 * Determine who did the damage
			 */
			Player damager = null;
			try{
				damager = (Player) GenUtil.getTrueCulprit((EntityDamageByEntityEvent) event);
			}
			catch(ClassCastException cce){}
			if(damager == null)
				return;
			/*
			 * Teammates can't hurt each other
			 */
			if(GameMaster.activeGame instanceof TeamAutoGame){
				TeamAutoGame game = (TeamAutoGame) GameMaster.activeGame;
				if(game.getTeam(victim) == game.getTeam(damager))
					event.setCancelled(true);
			}
			/*
			 * Safe spawn games
			 */
			if(GameMaster.activeGame instanceof SafeSpawnModule){
				SafeSpawnModule game = (SafeSpawnModule) GameMaster.activeGame;
				if(victim.getLocation().distance(game.getSafeLoc(victim)) < game.getSafeRadius(victim)){
					event.setCancelled(true);
					Chat.send(damager, new Message(Scheme.WARNING).then("You can't damage enemies in their spawn"));
				}
				if(damager.getLocation().distance(game.getSafeLoc(damager)) < game.getSafeRadius(damager) && victim.getLocation().distance(game.getSafeLoc(damager)) > game.getSafeRadius(damager)){
					event.setCancelled(true);
					Chat.send(damager, new Message(Scheme.WARNING).then("You can't attack enemies from your spawn"));
				}
			}
			/*
			 * Schedule to modify the damage so that the real damager (wolf owner, arrow shooter) gets the credit
			 */
			final Player fVictim = victim, fDamager = damager;
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
				fVictim.setLastDamageCause(new EntityDamageByEntityEvent(fVictim, fDamager, DamageCause.ENTITY_ATTACK, event.getDamage()));
			}});
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void entityDamageMonitor(EntityDamageEvent event){
		if(!(event.getEntity() instanceof Player))
			return;
		if(GameMaster.status.active && GameMaster.activeGame instanceof SafeSpawnModule){
			final Player player = (Player) event.getEntity();
			if(GameMaster.getStatus(player) == PlayerStatus.PLAYING && (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.PROJECTILE || event.getCause() == DamageCause.MAGIC || event.getCause() == DamageCause.POISON))
				GameMaster.lastDamage.put(player, System.currentTimeMillis());
		}
	}
	
	@EventHandler
	public void foodLevelChange(FoodLevelChangeEvent event){
		if(GameMaster.status != MasterStatus.RUNNING)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void playerJoin(final PlayerJoinEvent event){
		final Player player = event.getPlayer();
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
			switch(GameMaster.status){
				case PREP:
					Chat.send(player,
						new Message(Scheme.HIGHLIGHT).then("We're getting ready to play ").then(GameMaster.activeGame).strong(),
						new Message(Scheme.HIGHLIGHT).then("Click here").strong().style(ChatColor.BOLD).command("/vote").then(" to vote on the next map")
						);
					break;
				case INTERMISSION:
					Chat.send(player,
						new Message(Scheme.HIGHLIGHT).then("We're voting on the next game"),
						new Message(Scheme.HIGHLIGHT).then("Click here").strong().style(ChatColor.BOLD).command("/vote").then(" to vote on the next game")
						);
				default: }
		} }, 20);
		/*
		 * If the player is an admin, leave them be
		 */
		if(player.hasPermission("gamemaster.admin"))
			GameMaster.changeStatus(player, PlayerStatus.ADMIN);
		/*
		 * If the player is new, welcome them and send them to the newbie initiation room
		 */
		else if(!player.hasPlayedBefore()){
			GameMaster.changeStatus(player, PlayerStatus.EXTERIOR);
			//TODO Welcome player to server
		}
		/*
		 * Otherwise, just shove them headfirst into the games
		 */
		else{
			GameMaster.changeStatus(player, PlayerStatus.PLAYING);
			if(GameMaster.status.active)
				GameMaster.activeGame.addPlayer(player);
			else
				player.teleport(GameMaster.mainLobby);
		}
	}
	
	@EventHandler
	public void playerQuit(PlayerQuitEvent event){
		Player player = event.getPlayer();
		StatusBar.removeStatusBar(player);
		if(GameMaster.status.active && GameMaster.getStatus(player) == PlayerStatus.PLAYING){
			player.teleport(GameMaster.mainLobby);
			GameMaster.resetPlayer(player);
			GameMaster.activeGame.removePlayer(player);
			player.setPlayerListName(player.getName());
		}
	}
	
	@EventHandler
	public void playerKick(PlayerKickEvent event){
		playerQuit(new PlayerQuitEvent(event.getPlayer(), "simulation"));
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void playerRespawn(final PlayerRespawnEvent event){
		final Player player = event.getPlayer();
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		if(!GameMaster.status.active)
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
				player.teleport(GameMaster.mainLobby);
			}});
		else if(GameMaster.activeGame instanceof RespawnModule){
			final RespawnModule game = (RespawnModule) GameMaster.activeGame;
			/*
			 * If and only if the player is playing...
			 */
			if(GameMaster.getStatus(player) == PlayerStatus.PLAYING){
				/*
				 * First send them to the waiting location
				 */
				Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
					player.teleport(game.getWaitingLoc(player));
					GameMaster.respawning.add(player);
				}});
				/*
				 * After the delay has passed...
				 */
				Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.plugin(), new Runnable(){ public void run(){
					/*
					 * If they're still active, respawn them
					 */
					if(GameMaster.status.active && GameMaster.getStatus(player) == PlayerStatus.PLAYING && player.isOnline()){
						player.teleport(game.getRespawnLoc(player));
						player.setNoDamageTicks(20 * game.getRespawnInvuln(player));
						/*
						 * If the game also happens to be MessagerCompatible, send messages as well
						 */
						if(game instanceof MessagerModule){
							Chat.send(player, Align.addSpacers("", Align.center(((MessagerModule) game).getRespawnMessage(player))));
						}
						GameMaster.respawning.remove(player);
					}
				}}, 20 * game.getRespawnDelay(player));
				return;
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void playerDeath(EntityDeathEvent event){
		event.getDrops().clear();
		GameMaster.lastDamage.remove(event.getEntity());
		Player player = (Player) event.getEntity();
		StatusBar.removeStatusBar(player);
		if(splashHarms.containsKey(player) && (player.getLastDamageCause().getCause() == DamageCause.MAGIC || player.getLastDamageCause().getCause() == DamageCause.WITHER)){
			Player thrower = Bukkit.getPlayer(splashHarms.get(player));
			if(thrower != null){
				player.setLastDamageCause(new EntityDamageByEntityEvent(thrower, player, DamageCause.ENTITY_ATTACK, 10.0));
				splashHarms.remove(player);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void forbidChestStorage(InventoryClickEvent event){
		Player player = (Player) event.getWhoClicked();
		if(GameMaster.getStatus(player) != PlayerStatus.ADMIN && event.getView().getTopInventory() != null)
			switch(event.getView().getTopInventory().getType()){
				case CRAFTING: case CREATIVE: case PLAYER:
					event.setCancelled(false);
					break;
				default:
					event.setCancelled(true);
					break;
			}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void teamChat(AsyncPlayerChatEvent event){
		Player player = event.getPlayer();
		if(GameMaster.teamChatters.contains(player) && GameMaster.getStatus(player) == PlayerStatus.PLAYING && GameMaster.status.active && GameMaster.activeGame instanceof TeamAutoGame){
			TeamAutoGame game = (TeamAutoGame) GameMaster.activeGame;
			for(Player other : Bukkit.getOnlinePlayers())
				if(game.getTeam(player) != game.getTeam(other))
					event.getRecipients().remove(other);
			event.setMessage("(" + game.getTeam(player).chat + "TEAM" + ChatColor.WHITE + ") " + event.getMessage());
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void votifier(VotifierEvent event){
		Vote vote = event.getVote();
		if(vote == null){
			GameMaster.logger().severe("VotifierEvent returned null vote");
			return;
		}
		GameMaster.logger().info("Received vote from " + vote.getServiceName() + " by " + vote.getUsername() + " from " + vote.getAddress() + " at " + vote.getTimeStamp());
		OfflinePlayer player = Bukkit.getPlayer(vote.getUsername());
		if(player == null)
			player = Bukkit.getOfflinePlayer(vote.getUsername());
		if(player.hasPlayedBefore() || player.isOnline()){
			StatMaster.getHandler().incrementStat(player, "charges");
			StatMaster.getHandler().incrementCommunityStat("votes");
			Chat.broadcast(new Message(Scheme.HIGHLIGHT).then(player.getName()).strong().then(" voted for the server, and now has ").then(StatMaster.getHandler().getStat(player, "charges")).strong().then(" charges"));
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void restrictCommandKitsToSpawns(GiveKitEvent event){
		final Player player = event.getPlayer();
		if(GameMaster.getStatus(player) == PlayerStatus.PLAYING && GameMaster.status == MasterStatus.RUNNING)
			if(GameMaster.activeGame instanceof RespawnModule && player.getLocation().distance(((RespawnModule) GameMaster.activeGame).getRespawnLoc(player)) > 10)
				if(!event.getContext().overrides && event.getContext() != GiveKitContext.SIGN_TAKEN && !player.hasPermission("gamemaster.globalkit")){
					Chat.send(player, new Message(Scheme.WARNING).then("You must be in your spawn to take kits via command"));
					event.setCancelled(true);
					return;
				}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void killProjectilesOnKitChange(ClearKitsEvent event){
		Player player = event.getPlayer();
		for(World world : Bukkit.getWorlds())
			for(Entity e : world.getEntities())
				if(e instanceof Projectile){
					Projectile proj = (Projectile) e;
					if(player.equals(proj.getShooter()))
						proj.remove();
				}
	}
	
	private static PlayerMap<String> splashHarms = new PlayerMap<String>();
	@EventHandler
	public void managePotionSplashes(PotionSplashEvent event){
		ThrownPotion potion = event.getPotion();
		Player thrower = null;
		if(potion.getShooter() instanceof Player)
			thrower = (Player) potion.getShooter();
		if(thrower == null || GameMaster.status != MasterStatus.RUNNING)
			return;
		for(LivingEntity entity : event.getAffectedEntities()){
			if(entity instanceof Player){
				Player victim = (Player) entity;
				if(GameMaster.activeGame instanceof TeamAutoGame){
					TeamAutoGame game = (TeamAutoGame) GameMaster.activeGame;
					if(game.getTeam(thrower) != game.getTeam(victim)){
						if(victim.getLocation().distance(game.getSafeLoc(victim)) < game.getSafeRadius(victim)){
							event.setIntensity(victim, 0);
							Chat.send(thrower, new Message(Scheme.WARNING).then("You can't attack enemies in their spawn"));
						}
						if(thrower.getLocation().distance(game.getSafeLoc(thrower)) < game.getSafeRadius(thrower) && victim.getLocation().distance(game.getSafeLoc(thrower)) >= game.getSafeRadius(thrower)){
							event.setIntensity(victim, 0);
							Chat.send(thrower, new Message(Scheme.WARNING).then("You can't attack enemies from your spawn"));
						}
					}
					else if(game.getTeam(thrower) == game.getTeam(victim))
						for(PotionEffect effect : potion.getEffects())
							//List of all negative potion effect IDs (this is for friendly-fire)
							if(Lists.newArrayList(2,4,7,9,15,17,18,19,20).contains(effect.getType().getId()))
								event.setIntensity(victim, 0);
				}
				for(PotionEffect effect : potion.getEffects())
					if(effect.getType().equals(PotionEffectType.HARM) || effect.getType().equals(PotionEffectType.WITHER))
						splashHarms.put(victim, thrower.getName());
			}
		}
	}
	
	@EventHandler
	public void updateServerList(ServerListPingEvent event){
		switch(GameMaster.status){
			case INTERMISSION:
				event.setMotd(new Message(Scheme.HIGHLIGHT).then("Voting on the next game").toString());
				break;
			case PREP:
				event.setMotd(new Message(Scheme.HIGHLIGHT).then("Voting on a map for ").then(GameMaster.activeGame).strong().toString());
				break;
			case RUNNING:
			case SUSPENDED:
				event.setMotd(new Message(Scheme.HIGHLIGHT).then("Playing ").then(GameMaster.activeGame).strong().then(" on ").then(GameMaster.activeMap).strong().toString());
				break;
		}
	}
	
}
