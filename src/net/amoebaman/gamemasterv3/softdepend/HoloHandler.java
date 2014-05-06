package net.amoebaman.gamemasterv3.softdepend;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dsh105.holoapi.HoloAPI;
import com.dsh105.holoapi.api.Hologram;

import net.amoebaman.gamemasterv3.GameMaster;
import net.amoebaman.gamemasterv3.util.Utils;
import net.amoebaman.amoebautils.maps.PlayerMap;

public class HoloHandler{
	
	private PlayerMap<Hologram> statusHolos;
	private GameMaster plugin;
	
	public HoloHandler(GameMaster plugin){
		this.plugin = plugin;
	}
	
	public void sendHoloMessage(final Player player, List<String> messages, int duration){
		if(!statusHolos.containsKey(player)){
			final Hologram holo = HoloAPI.getManager().createSimpleHologram(Utils.getHoloHudLoc(player), duration, messages);
			holo.clearAllPlayerViews();
			holo.show(player);
			statusHolos.put(player, holo);
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable(){ public void run(){
				holo.clearAllPlayerViews();
				HoloAPI.getManager().stopTracking(holo);
				statusHolos.remove(player);
			}}, duration * 20);
		}
	}
	
	public void moveStatusHolo(Player player){
		if(statusHolos.containsKey(player))
			statusHolos.get(player).move(Utils.getHoloHudLoc(player).add(new Vector(0, 0.25 * statusHolos.get(player).getLines().length, 0)));
	}

}
