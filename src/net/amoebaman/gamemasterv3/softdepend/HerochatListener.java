package net.amoebaman.gamemasterv3.softdepend;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.dthielke.herochat.ChannelChatEvent;

import net.amoebaman.gamemasterv3.GameMaster;
import net.amoebaman.gamemasterv3.api.TeamAutoGame;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.gamemasterv3.enums.Team;

public class HerochatListener implements Listener{
	
	private static Set<Player> teamChatting = new HashSet<Player>();
	private GameMaster master;
	
	public HerochatListener(GameMaster master){
		this.master = master;
	}
	
	public static boolean toggleTeamChat(Player player){
		if(!teamChatting.remove(player)){
			teamChatting.add(player);
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onChannelChat(ChannelChatEvent event){
		Player player = event.getSender().getPlayer();
		if(master.getState(player) == PlayerState.PLAYING){
			event.setChannel(master.getMainChannel());
			if(teamChatting.contains(player) && master.getState() != GameState.INTERMISSION && master.getActiveGame() instanceof TeamAutoGame){
				TeamAutoGame game = (TeamAutoGame) master.getActiveGame();
				Team team = game.getTeam(player);
				event.setChannel(game.getChannel(team));
			}
		}
		if(master.getState(player) == PlayerState.WATCHING)
			event.setChannel(master.getSpectatorChannel());
	}
}
