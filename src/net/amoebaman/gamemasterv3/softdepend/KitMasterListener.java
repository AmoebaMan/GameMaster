package net.amoebaman.gamemasterv3.softdepend;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.amoebaman.gamemasterv3.GameMaster;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.gamemasterv3.modules.SafeSpawnModule;
import net.amoebaman.kitmaster.enums.GiveKitContext;
import net.amoebaman.kitmaster.utilities.ClearKitsEvent;
import net.amoebaman.kitmaster.utilities.GiveKitEvent;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;

public class KitMasterListener implements Listener{
	
	private GameMaster master;
	
	public KitMasterListener(GameMaster master){
		this.master = master;
	}
	
	@EventHandler
	public void killProjectilesOnKitChange(ClearKitsEvent event){
		Player player = event.getPlayer();
		if(master.getState(player) != PlayerState.EXTERIOR)
			for(Projectile proj : player.getWorld().getEntitiesByClass(Projectile.class))
				if(player.equals(proj.getShooter()))
					proj.remove();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void restrictCommandKitsToSpawns(GiveKitEvent event){
		final Player player = event.getPlayer();
		if(master.getState(player) == PlayerState.PLAYING && master.getState() == GameState.RUNNING){
			if(master.getActiveGame() instanceof SafeSpawnModule){
				SafeSpawnModule game = (SafeSpawnModule) master.getActiveGame();
				if(player.getLocation().distance(game.getSafeLoc(player)) > game.getSafeRadius(player))
					if(!event.getContext().overrides && event.getContext() != GiveKitContext.SIGN_TAKEN && !player.hasPermission("gamemaster.globalkit")){
						new Message(Scheme.WARNING).then("You must be in your spawn to take kits via command").send(player);
						event.setCancelled(true);
					}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void forbidChestStorage(InventoryClickEvent event){
		Player player = (Player) event.getWhoClicked();
		if(master.getState(player) != PlayerState.EXTERIOR && event.getView().getTopInventory() != null)
			switch(event.getView().getTopInventory().getType()){
				case CRAFTING:
				case CREATIVE:
				case PLAYER:
					event.setCancelled(false);
					break;
				default:
					event.setCancelled(true);
					break;
			}
	}
}
