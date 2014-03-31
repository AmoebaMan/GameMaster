package net.amoebaman.gamemasterv3.softdepend;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import net.amoebaman.gamemasterv3.GameMaster;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;

import net.milkbowl.vault.economy.Economy;

public class VotifierListener implements Listener{
	
	private GameMaster master;
	
	public VotifierListener(GameMaster master){
		this.master = master;
	}
	
	@EventHandler
	public void votifier(VotifierEvent event){
		/*
		 * Logging
		 */
		Vote vote = event.getVote();
		if(vote == null){
			master.log("VotifierEvent returned null vote");
			return;
		}
		master.log("Received vote from " + vote.getServiceName() + " by " + vote.getUsername() + " from " + vote.getAddress() + " at " + vote.getTimeStamp());
		/*
		 * Get whoever it was that voted
		 */
		OfflinePlayer player = Bukkit.getPlayer(vote.getUsername());
		if(player == null)
			player = Bukkit.getOfflinePlayer(vote.getUsername());
		/*
		 * Do stuff
		 */
		if(Depend.hasEconomy()){
			Economy econ = Depend.getEconomy();
			if(econ.hasAccount(player.getName())){
				econ.depositPlayer(player.getName(), master.getConfig().getDouble("currency.reward-per-vote"));
				master.broadcast(new Message(Scheme.HIGHLIGHT)
					.t(player.getName()).s()
					.t(" voted for the server, and now has ")
					.t(econ.getBalance(player.getName())).s()
					.t(" " + econ.currencyNamePlural()));
			}
		}
	}
}
