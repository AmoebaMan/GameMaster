package com.amoebaman.gamemaster;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;	
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.kitteh.tag.PlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;

import com.amoebaman.gamemaster.api.TeamAutoGame;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.enums.PlayerStatus;
import com.amoebaman.gamemaster.handlers.ChargeHandler;
import com.amoebaman.gamemaster.objects.PlayerMap;
import com.amoebaman.gamemaster.utils.ChatUtils;
import com.amoebaman.gamemaster.utils.Utils;
import com.amoebaman.kitmaster.enums.Attribute;
import com.amoebaman.kitmaster.enums.GiveKitContext;
import com.amoebaman.kitmaster.handlers.HistoryHandler;
import com.amoebaman.kitmaster.objects.GiveKitEvent;
import com.amoebaman.kitmaster.objects.Kit;
import com.google.common.collect.Sets;
import com.vexsoftware.votifier.model.VotifierEvent;

public class GameMasterListener implements Listener{

	public static void init(GameMaster master){
		Bukkit.getPluginManager().registerEvents(new GameMasterListener(), master);
	}
	
	@EventHandler
	public void disallowBlockPlacement( BlockPlaceEvent event){
		if(GameMaster.players.get(event.getPlayer()) != PlayerStatus.NOT_PLAYING)
			event.setCancelled(true);
	}

	@EventHandler
	public void disallowBlockBreaking(BlockBreakEvent event){
		if(GameMaster.players.get(event.getPlayer()) != PlayerStatus.NOT_PLAYING)
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void regulateDamage(final EntityDamageEvent event){
		if(GameMaster.status != MasterStatus.RUNNING || GameMaster.players.get(event.getEntity()) != PlayerStatus.PLAYING)
			event.setCancelled(true);
		Player victim = null;
		if(event.getEntity() instanceof Player)
			victim = (Player) event.getEntity();
		if(event.getEntity() instanceof Wolf && ((Wolf) event.getEntity()).getOwner() instanceof Player)
			victim = (Player) ((Wolf) event.getEntity()).getOwner();
		if(victim == null)
			return;
		if(event instanceof EntityDamageByEntityEvent && GameMaster.players.get(victim) == PlayerStatus.PLAYING){
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
			final Player finalVictim = victim, finalDamager = damager;
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.getPlugin(), new Runnable(){ public void run(){
				finalVictim.setLastDamageCause(new EntityDamageByEntityEvent(finalVictim, finalDamager, DamageCause.ENTITY_ATTACK, event.getDamage()));
			}});
		}
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void trackDamage(EntityDamageEvent event){
		final Player player;
		try{
			player = (Player) event.getEntity();
		}
		catch(ClassCastException cce){ return; }
		if(event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.PROJECTILE || event.getCause() == DamageCause.MAGIC){
			GameMaster.recentlyDamaged.add(player);
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.getPlugin(), new Runnable(){ public void run(){
				GameMaster.recentlyDamaged.remove(player);
			}}, 200);
		}
		if(event.getCause() != DamageCause.MAGIC)
			splashHarms.remove(player);
	}

	@EventHandler
	public void onPlayerFoodLevelChange(FoodLevelChangeEvent event){
		switch(GameMaster.players.get((Player) event.getEntity())){
		case PLAYING:
			if(GameMaster.status != MasterStatus.RUNNING)
				event.setCancelled(true);
			break;
		case SPECTATING:
			event.setCancelled(true);
		default: }
	}

	@EventHandler
	public void handlePlayerJoining(final PlayerJoinEvent event){
		final Player player = event.getPlayer();
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.getPlugin(), new Runnable(){ public void run(){
			switch(GameMaster.status){
			case PREP:
				player.sendMessage(ChatColor.GOLD + "The server is currently preparing to launch " + GameMaster.activeGame.getGameName());
				player.sendMessage(ChatColor.GOLD + "Type '/help games' for more info");
				break;
			case INTERMISSION:
				player.sendMessage(ChatColor.GOLD + "The server is waiting to begin the next event");
			default: }
			if(Bukkit.getOnlinePlayers().length > 0){
				HashSet<Player> setPlayers = Sets.newHashSet(Bukkit.getOnlinePlayers());	
				TagAPI.refreshPlayer(player, setPlayers);
			}
		} }, 20);
		if(player.hasPermission("gamemaster.autoexit"))
			GameMaster.players.put(player, PlayerStatus.NOT_PLAYING);
		else if(!player.hasPlayedBefore())
			GameMaster.players.put(player, PlayerStatus.SPECTATING);
		else{
			GameMaster.players.put(player, PlayerStatus.PLAYING);
			if(GameMaster.status.isActive)
				GameMaster.activeGame.insertPlayer(player);
		}
		for(OfflinePlayer op : Bukkit.getOperators())
			if(op.isOnline())
				((Player) op).playEffect(((Player) op).getLocation(), Effect.GHAST_SHRIEK, 0);
	}

	@EventHandler
	public void handlePlayerQuitting(PlayerQuitEvent event){
		Player player = event.getPlayer();
		if(!GameMaster.players.get(player).isActive)
			return;
		if(GameMaster.status.isActive){
			GameMaster.activeGame.extractPlayer(player);
			Utils.resetPlayerStatus(player, true);
			player.teleport(GameMaster.waitingRoom);
		}
		for(OfflinePlayer op : Bukkit.getOperators())
			if(op.isOnline())
				((Player) op).playEffect(((Player) op).getLocation(), Effect.POTION_BREAK, 0);
	}

	@EventHandler
	public void handlePlayerKicking(PlayerKickEvent event){
		Player player = event.getPlayer();
		if(!GameMaster.players.get(player).isActive)
			return;
		if(GameMaster.status.isActive){
			GameMaster.activeGame.extractPlayer(player);
			Utils.resetPlayerStatus(player, true);
			player.teleport(GameMaster.waitingRoom);
		}
	}

	@EventHandler
	public void handleRespawning(final PlayerRespawnEvent event){
		final Player player = event.getPlayer();
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameMaster.getPlugin(), new Runnable(){ public void run(){
			player.teleport(GameMaster.waitingRoom);
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
		}});
		if(GameMaster.status.isActive){
			GameMaster.players.put(player, PlayerStatus.RESPAWNING);
			GameMaster.awaitingRespawn.put(player, System.currentTimeMillis());
		}
	}

	@EventHandler
	public void manageDeaths(EntityDeathEvent event){
		event.getDrops().clear();
		if(event instanceof PlayerDeathEvent){
			Player player = (Player) event.getEntity();
			GameMaster.recentlyDamaged.remove(player);
			Utils.clearInventory(player);
			if(splashHarms.containsKey(player)){
				Player thrower = Bukkit.getPlayer(splashHarms.get(player));
				if(thrower != null){
					player.setLastDamageCause(new EntityDamageByEntityEvent(thrower, player, DamageCause.ENTITY_ATTACK, 10));
					splashHarms.remove(player);
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void preventChestAbuse(InventoryClickEvent event){
		Player player = (Player) event.getWhoClicked();
		if(GameMaster.players.get(player) != PlayerStatus.NOT_PLAYING){
			Inventory topInv = event.getView().getTopInventory();
			if(topInv != null && (topInv.getHolder() instanceof BlockState || topInv.getHolder() instanceof Vehicle))
				for(Kit kit : HistoryHandler.getHistory(player)){
					for(ItemStack item : kit.items)
						if(event.getCurrentItem() != null && event.getCurrentItem().getType() == item.getType())
							event.setCancelled(true);
				}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void disallowBroadcastFlooding(AsyncPlayerChatEvent event){
		Player player = event.getPlayer();
		if(!player.hasPermission("gamemaster.broadcast") && System.currentTimeMillis() < GameMaster.lastBroadcast + 5000){
			event.setCancelled(true);
			player.sendMessage(ChatUtils.ERROR + "You can't speak directly after a broadcast");
		}
	}

	@EventHandler
	public void handleTeamChatting(AsyncPlayerChatEvent event){
		Player player = event.getPlayer();
		if(Utils.inTeamChat(player) && GameMaster.players.get(player).isActive && GameMaster.status.isActive && GameMaster.activeGame instanceof TeamAutoGame){
			TeamAutoGame game = (TeamAutoGame) GameMaster.activeGame;
			for(Player other : Bukkit.getOnlinePlayers())
				if(game.getTeam(player) != game.getTeam(other))
					event.getRecipients().remove(other);
			event.setMessage("(" + game.getTeam(player).chat + "TEAM" + ChatColor.WHITE + ") " + event.getMessage());
		}
	}

	@EventHandler
	public void handlePlayerTags(PlayerReceiveNameTagEvent event){
		Player player = event.getNamedPlayer();
		if(GameMaster.players.get(player).isActive && GameMaster.status.isActive)
			event.setTag(player.getDisplayName());
	}

	@EventHandler(priority=EventPriority.LOW)
	public void disallowPickups(PlayerPickupItemEvent event){
		Player player = event.getPlayer();
		if(GameMaster.players.get(player) != PlayerStatus.NOT_PLAYING){
			event.setCancelled(true);
			for(Kit kit : HistoryHandler.getHistory(player))
				if(kit != null)
					for(ItemStack stack : kit.items)
						if(stack.getType() == event.getItem().getItemStack().getType())
							event.setCancelled(false);
		}
	}

	@EventHandler
	public void disallowWeatherChanges(WeatherChangeEvent event){
		if(GameMaster.status.isActive)
			event.setCancelled(true);
	}

	@EventHandler
	public void disallowWeatherChanges(ThunderChangeEvent event){
		if(GameMaster.status.isActive)
			event.setCancelled(true);
	}

	@EventHandler
	public void autoTameWolves(CreatureSpawnEvent event){
		if(event.getEntityType() == EntityType.WOLF && (event.getSpawnReason() == SpawnReason.BREEDING || event.getSpawnReason() == SpawnReason.SPAWNER_EGG)){
			Wolf wolf = (Wolf) event.getEntity();
			for(Entity other : event.getEntity().getNearbyEntities(4, 4, 4))
				if(other instanceof Player){
					Player tamer = (Player) other;
					if(tamer.getInventory().contains(Material.BONE)){
						wolf.setOwner(tamer);
						wolf.setHealth(wolf.getMaxHealth());
						wolf.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1, 600));
						wolf.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1, 600));
						wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 600));
					}
				}
		}
	}

	@EventHandler
	public void handleVotifier(VotifierEvent event){
		if(event.getVote() == null){
			GameMaster.logger.severe("VotifierEvent returned null vote");
			return;
		}
		OfflinePlayer player = Bukkit.getOfflinePlayer(event.getVote().getUsername());
		if(player == null){
			GameMaster.logger.severe("VotifierEvent returned null player");
			return;
		}
		ChargeHandler.adjustCharges(player, 1);
		Bukkit.broadcastMessage(ChatUtils.HIGHLIGHT + ChatUtils.highlightEmphasis(player.getName()) + " voted for the server, and now has " + ChatUtils.highlightEmphasis(ChargeHandler.getCharges(player)) + " charges");
	}

	@EventHandler
	public void regulateKits(GiveKitEvent event){
		final Player player = event.getPlayer();
		switch(GameMaster.players.get(player)){
		case PLAYING:
			if(GameMaster.status == MasterStatus.RUNNING){
				if(Utils.spawnDistance(player) > 10 && !player.hasPermission("gamemaster.globalkit") && !event.getContext().overrides && event.getContext() != GiveKitContext.SIGN_TAKEN && GameMaster.activeGame instanceof TeamAutoGame){
					player.sendMessage(ChatUtils.ERROR + "You must be within 10 blocks of your spawn to choose a kit.");
					event.setCancelled(true);
					return;
				}
				if(!event.isCancelled() && (event.getKit().booleanAttribute(Attribute.CLEAR_ALL) || event.getKit().booleanAttribute(Attribute.CLEAR_INVENTORY))){
					for(Entity entity : player.getWorld().getEntities()){
						if(entity instanceof Tameable){
							Tameable pet = (Tameable) entity;
							if(pet.getOwner() == null || pet.getOwner().equals(player))
								while(!entity.isDead())
									((LivingEntity) entity).damage(20);
						}
						if(entity instanceof Projectile){
							Projectile proj = (Projectile) entity;
							if(proj.getShooter() == null || proj.getShooter().equals(player))
								proj.remove();
						}
					}
				}
			}
			break;
		default: }
	}

	@EventHandler
	public void preventIrritation(PlayerInteractEvent event){
		if(GameMaster.players.get(event.getPlayer()) != PlayerStatus.NOT_PLAYING && event.getAction().name().contains("CLICK_BLOCK")){
			Material mat = event.getClickedBlock().getType();
			if(mat == Material.TRAP_DOOR || mat == Material.LEVER)
				event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void disallowBreakingHangingBlocks(HangingBreakByEntityEvent event){
		Player player = null;
		if(event.getEntity() instanceof Player)
			player = (Player) event.getEntity();
		if(event.getEntity() instanceof Projectile){
			Projectile proj = (Projectile) event.getEntity();
			if(proj.getShooter() instanceof Player)
				player = (Player) proj.getShooter();
		}
		if(player == null)
			return;
		if(GameMaster.players.get(player) != PlayerStatus.NOT_PLAYING)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void disallowMinecartGrief(VehicleDamageEvent event){
		Player player = null;
		if(event.getAttacker() instanceof Player)
			player = (Player) event.getAttacker();
		if(event.getAttacker() instanceof Projectile){
			Projectile proj = (Projectile) event.getAttacker();
			if(proj.getShooter() instanceof Player)
				player = (Player) proj.getShooter();
		}
		if(player == null)
			return;
		if(GameMaster.players.get(player) != PlayerStatus.NOT_PLAYING && event.getVehicle() instanceof Minecart)
			event.setCancelled(true);
	}
	
	private static PlayerMap<String> splashHarms = new PlayerMap<String>();
	@EventHandler
	public void trackPotionHarms(PotionSplashEvent event){
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
						if(Utils.spawnDistance(victim) < 7){
							event.setCancelled(true);
							thrower.sendMessage(ChatUtils.ERROR + "You can't damage enemies that are in their spawn");
						}
						if(thrower.getLocation().distance(GameMaster.activeGame.getRespawnLoc(thrower)) < 7 && victim.getLocation().distance(GameMaster.activeGame.getRespawnLoc(thrower)) >= 7){
							event.setCancelled(true);
							thrower.sendMessage(ChatUtils.ERROR + "You can't damage enemies out of your spawn");
						}
					}
				}
				if(!event.isCancelled())
					for(PotionEffect effect : potion.getEffects())
						if(effect.getType().equals(PotionEffectType.HARM))
							splashHarms.put(victim, thrower.getName());
			}
		}
	}

	@EventHandler
	public void disallowItemDropping(PlayerDropItemEvent event){
		if(GameMaster.players.get(event.getPlayer()) != PlayerStatus.NOT_PLAYING)
			event.setCancelled(true);
	}
	
	private static PlayerMap<Long> teleports = new PlayerMap<Long>(0L);
	@EventHandler
	public void preventEnderPearlSpam(PlayerTeleportEvent event){
		Player player = event.getPlayer();
		if(GameMaster.players.get(player).isActive && event.getCause() == TeleportCause.ENDER_PEARL){
			if(System.currentTimeMillis() - teleports.get(player) < 3000){
				player.sendMessage(ChatUtils.ERROR + "You need to wait " + (int)((teleports.get(player) + 3000 - System.currentTimeMillis()) / 1000) + " seconds to teleport again");
				event.setCancelled(true);
			}
			else
				teleports.put(player, System.currentTimeMillis());
		}
	}
	
	@EventHandler
	public void updateServerList(ServerListPingEvent event){
		String filler = "" + ChatColor.GOLD + ChatColor.ITALIC;
		switch(GameMaster.status){
		case INTERMISSION:
			event.setMotd(filler + "Voting on the next game");
			break;
		case PREP:
			event.setMotd(filler + "Voting on a map for " + ChatColor.DARK_RED + GameMaster.activeGame.getGameName());
			break;
		case RUNNING:
		case SUSPENDED:
			event.setMotd(filler + "Playing " + ChatColor.DARK_RED + GameMaster.activeGame.getGameName() + filler + " on " + ChatColor.DARK_RED + GameMaster.activeGame.getActiveMap().name);
			break;
		}
	}
}
