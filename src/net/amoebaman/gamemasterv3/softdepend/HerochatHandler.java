package net.amoebaman.gamemasterv3.softdepend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.dthielke.herochat.*;

import net.amoebaman.gamemasterv3.GameMaster;
import net.amoebaman.gamemasterv3.api.TeamAutoGame;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.gamemasterv3.enums.Team;

public class HerochatHandler implements Listener{
	
	private Set<Player> teamChatting = new HashSet<Player>();
	private Channel gameChannel, spectatorChannel;
	private Map<Team, Channel> teamChannels = new HashMap<Team, Channel>();
	private GameMaster master;
	
	public HerochatHandler(GameMaster master){
		this.master = master;
		ChannelManager hc = Herochat.getChannelManager();
		gameChannel = master.getConfig().getBoolean("wrap-server")
			? hc.getDefaultChannel()
				: hc.getChannel("gamemaster");
			if(gameChannel == null){
				gameChannel = new StandardChannel(hc.getStorage(), "gamemaster", "gm", hc.getDefaultChannel().getFormatSupplier());
				gameChannel.setColor(ChatColor.WHITE);
				gameChannel.setVerbose(false);
				hc.addChannel(gameChannel);
			}
			spectatorChannel = hc.getChannel("spectator");
			if(spectatorChannel == null){
				spectatorChannel = new StandardChannel(hc.getStorage(), "spectator", "spc", hc.getDefaultChannel().getFormatSupplier());
				spectatorChannel.setColor(ChatColor.GRAY);
				spectatorChannel.setVerbose(false);
				hc.addChannel(spectatorChannel);
			}
	}	

	public Channel getMainChannel(){
		return gameChannel;
	}
	
	public Channel getSpectatorChannel(){
		return spectatorChannel;
	}

	/**
	 * Gets the chat channel reserved for this team's private communications.
	 * 
	 * @param team a team
	 * @return the team's chat channel
	 */
	public Channel getTeamChannel(Team team){
		if(team == null)
			return getMainChannel();
		else{
			if(!teamChannels.containsKey(team)){
				ChannelManager hc = Herochat.getChannelManager();
				Channel teamChannel = hc.getChannel(team + "Team");
				if(teamChannel == null){
					teamChannel = new StandardChannel(hc.getStorage(), team + "Team", team.toString(), hc.getDefaultChannel().getFormatSupplier());
					teamChannel.setColor(team.chat);
					teamChannel.setVerbose(false);
					hc.addChannel(teamChannel);
				}
				teamChannels.put(team, teamChannel);
			}
			return teamChannels.get(team);
		}
	}
	
	public boolean toggleTeamChat(Player player){
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
			event.setChannel(getMainChannel());
			if(teamChatting.contains(player) && master.getState() != GameState.INTERMISSION && master.getActiveGame() instanceof TeamAutoGame){
				TeamAutoGame game = (TeamAutoGame) master.getActiveGame();
				Team team = game.getTeam(player);
				event.setChannel(master.getHerochatHandler().getTeamChannel(team));
			}
		}
		if(master.getState(player) == PlayerState.WATCHING)
			event.setChannel(getSpectatorChannel());
	}

}
