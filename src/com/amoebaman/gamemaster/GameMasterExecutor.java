package com.amoebaman.gamemaster;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.kitteh.tag.TagAPI;



import com.amoebaman.gamemaster.api.AutoGame;
import com.amoebaman.gamemaster.api.GameMap;
import com.amoebaman.gamemaster.api.TeamAutoGame;
import com.amoebaman.gamemaster.enums.MasterStatus;
import com.amoebaman.gamemaster.enums.PlayerStatus;
import com.amoebaman.gamemaster.handlers.ChargeHandler;
import com.amoebaman.gamemaster.utils.ChatUtils;
import com.amoebaman.gamemaster.utils.CommandController;
import com.amoebaman.gamemaster.utils.CommandController.CommandHandler;
import com.amoebaman.gamemaster.utils.Utils;
import com.amoebaman.kitmaster.KitMaster;
import com.amoebaman.kitmaster.enums.Attribute;
import com.amoebaman.kitmaster.enums.GiveKitContext;
import com.amoebaman.kitmaster.handlers.HistoryHandler;
import com.amoebaman.kitmaster.handlers.InventoryHandler;
import com.amoebaman.kitmaster.handlers.KitHandler;
import com.amoebaman.kitmaster.objects.Kit;

public class GameMasterExecutor{

	public static GameMaster plugin;
	public static void init(GameMaster gm){
		plugin = gm;
		GameMasterExecutor instance = new GameMasterExecutor();
		CommandController.registerCommands(gm, instance);
	}
	
	@CommandHandler(name = "game")
	public void game(CommandSender sender, String[] args){
		if(GameMaster.activeGame == null || !GameMaster.activeGame.isActive()){
			sender.sendMessage(ChatUtils.ERROR + "There isn't a game running");
			return;
		}
		Utils.message(sender, GameMaster.activeGame.getStatus(sender instanceof Player ? (Player) sender : null)); 
	}
	
	@CommandHandler(name = "vote")
	public void vote(CommandSender sender, String[] args){
		switch(GameMaster.status){
		case INTERMISSION:
			if(GameMaster.getRegisteredGames().size() < 2){
				sender.sendMessage(ChatUtils.ERROR + "There aren't any games to vote for");
				return;
			}
			AutoGame game = args.length > 0 ? GameMaster.getRegisteredGame(args[0]) : null;
			if(game == null){
				sender.sendMessage(ChatUtils.ERROR + "Choose a valid game to vote for");
				sender.sendMessage(ChatUtils.NORMAL + "Available games: " + GameMaster.getRegisteredGames());
				return;
			}
			if(game.equals(GameMaster.lastGame)){
				sender.sendMessage(ChatUtils.ERROR + "You can't vote for the game that just ran");
				return;
			}
			GameMaster.votes.put(sender, game.getGameName());
			sender.sendMessage(ChatUtils.NORMAL + "You voted " + ChatColor.DARK_GRAY + game.getGameName() + ChatUtils.NORMAL + " for the next event");
			break;
		case PREP:
			GameMap map = args.length > 0 ? GameMaster.activeGame.getMap(args[0]) : null;
			if(map == null){
				sender.sendMessage(ChatUtils.ERROR + "Choose a valid map to vote for");
				sender.sendMessage(ChatUtils.NORMAL + "Available maps: " + GameMaster.activeGame.getMaps());
				return;
			}
			if(GameMaster.recentMaps.contains(map)){
				sender.sendMessage(ChatUtils.ERROR + "That map has already played recently, choose another");
				return;
			}
			GameMaster.votes.put(sender, map.name);
			sender.sendMessage(ChatUtils.NORMAL + "You voted " + ChatColor.DARK_GRAY + map.name + ChatUtils.NORMAL + " for the next map");
			break;
		case RUNNING:
		case SUSPENDED:
			sender.sendMessage(ChatUtils.ERROR + "You can only vote on games or maps during the intermission");
			return;	
		}
		return;
	}
	
	@CommandHandler(name = "charges")
	public void charges(CommandSender sender, String[] args){
		
		if(args == null || args.length < 1){
			ChatUtils.sendSpacerLine(sender);
			if(sender instanceof Player)
				sender.sendMessage(ChatUtils.HIGHLIGHT + "You have " + ChatUtils.highlightEmphasis(ChargeHandler.getCharges((Player) sender)) + " charges");
			sender.sendMessage(ChatUtils.NORMAL + "Earn charges by voting for us once per day");
			sender.sendMessage(ChatUtils.HIGHLIGHT + "  bit.ly/landwarvotepmc");
			sender.sendMessage(ChatUtils.HIGHLIGHT + "  bit.ly/landwarvotems");
			sender.sendMessage(ChatUtils.NORMAL + "Use charges with " + ChatUtils.normalEmphasis("/charges use") + " to upgrade kits");
			sender.sendMessage(ChatUtils.NORMAL + "Get info about a charged kit with " + ChatUtils.normalEmphasis("/charges info <kit>"));
			ChatUtils.sendSpacerLine(sender);
			return;
		}
		
		if(args[0].equalsIgnoreCase("use") && sender instanceof Player){
			Player player = (Player) sender;
			if(ChargeHandler.getCharges(player) < 1){
				sender.sendMessage(ChatUtils.ERROR + "You don't have any charges");
				return;
			}
			List<Kit> last = HistoryHandler.getHistory(player);
			if(last == null || last.isEmpty() || last.get(0).name.toLowerCase().startsWith("t-")){
				sender.sendMessage(ChatUtils.ERROR + "You haven't taken a kit to use the charge on");
				return;
			}
			Kit charged = KitHandler.getKit("c-" + last.get(0).name);
			if(charged == null)
				charged = KitHandler.getKitByIdentifier("c-" + last.get(0).name);
			if(charged == null){
				sender.sendMessage(ChatUtils.ERROR + "Your current kit doesn't have an upgraded state available");
				return;
			}
			ChargeHandler.adjustCharges(player, -1);
			KitMaster.giveKit(player, charged, GiveKitContext.PLUGIN_GIVEN_OVERRIDE);
			sender.sendMessage(ChatUtils.NORMAL + "You used a charge to upgrade your kit");
			return;
		}
		
		if(args[0].equalsIgnoreCase("info")){
			if(args.length < 2){
				sender.sendMessage(ChatUtils.ERROR + "Name a kit to get info about its charged state");
				return;
			}
			Kit charged = KitHandler.getKit("c-" + args[1]);
			if(charged == null)
				charged = KitHandler.getKitByIdentifier("c-" + args[1]);
			if(charged == null){
				sender.sendMessage(ChatUtils.ERROR + "That kit doesn't have an upgraded state available");
				return;
			}
			sender.sendMessage(ChatColor.ITALIC + "Kit info for " + charged.name);
			sender.sendMessage(ChatColor.ITALIC + "Items:");
			for(ItemStack item : charged.items)
				sender.sendMessage(ChatColor.ITALIC + "  - " + InventoryHandler.friendlyItemString(item));
			sender.sendMessage(ChatColor.ITALIC + "Effects:");
			for(PotionEffect effect : charged.effects)
				sender.sendMessage(ChatColor.ITALIC + "  - " + InventoryHandler.friendlyEffectString(effect));
			sender.sendMessage(ChatColor.ITALIC + "Permissions:");
			for(String perm : charged.permissions)
				sender.sendMessage(ChatColor.ITALIC + "  - " + perm);
			for(Attribute attribute : charged.attributes.keySet())
				sender.sendMessage(ChatColor.ITALIC + attribute.toString() + ": " + charged.getAttribute(attribute));
		}
		
		if(args[0].equalsIgnoreCase("set") && sender.hasPermission("gamemaster.admin")){
			Player target = Bukkit.getPlayer(args[1]);
			if(target == null){
				sender.sendMessage(ChatUtils.ERROR + "Could not find player");
				return;
			}
			int amount = Integer.parseInt(args[2]);
			ChargeHandler.setCharges(target, amount);
			sender.sendMessage(ChatUtils.NORMAL + ChatUtils.normalEmphasis(target.getName()) + " now has " + ChatUtils.normalEmphasis(amount) + " charges");
			return;
		}
		
		Player other = Bukkit.getPlayer(args[0]);
		if(other != null){
			sender.sendMessage(ChatUtils.NORMAL + ChatUtils.normalEmphasis(other.getName()) + " has " + ChatUtils.normalEmphasis(ChargeHandler.getCharges(other)) + " charges");
			return;
		}
	}

	@CommandHandler(name = "teamchat")
	public void teamchat(Player player, String[] args){
		Utils.toggleTeamChat(player);
		player.sendMessage(ChatUtils.NORMAL + "Team-exclusive chatting is " + ChatUtils.normalEmphasis(Utils.inTeamChat(player) ? "enabled" : "disabled"));	
	}

	@CommandHandler(name = "fixme")
	public void fixme(Player player, String[] args){
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tp " + player.getName() + " " + player.getName());
		player.sendMessage(ChatUtils.NORMAL + "Fixed");
	}
	
	@CommandHandler(name = "swapteam")
	public void swapteam(CommandSender sender, String[] args){
		if(GameMaster.status != MasterStatus.RUNNING){
			sender.sendMessage(ChatUtils.ERROR + "There isn't a game running");
			return;
		}
		if(!(GameMaster.activeGame instanceof TeamAutoGame)){
			sender.sendMessage(ChatUtils.ERROR + "The current game doesn't utilize teams");
			return;
		}
		if(args.length == 0 && !(sender instanceof Player)){
			sender.sendMessage(ChatUtils.ERROR + "Specify a player to swap");
			return;
		}
		Player target = args.length == 0 ? (Player) sender : Bukkit.getPlayer(args[0]);
		if(target == null){
			sender.sendMessage(ChatUtils.ERROR + "Could not find target player");
			return;
		}
		if(sender.hasPermission("gamemaster.admin")){
			if(!target.equals(sender))
				sender.sendMessage(ChatUtils.NORMAL + "Swapping " + target.getName() + "'s team");
			else
				target.sendMessage(ChatUtils.NORMAL + "Your team has been changed");
		}
		((TeamAutoGame) GameMaster.activeGame).swapTeam(target);
		TagAPI.refreshPlayer(target);
	}

	@CommandHandler(name = "balanceteams")
	public void balanceteams(CommandSender sender, String[] args){
		TeamAutoGame.balanceTeams = !TeamAutoGame.balanceTeams;
		sender.sendMessage(ChatUtils.NORMAL + "Automatic team balancing is " + ChatUtils.normalEmphasis(TeamAutoGame.balanceTeams ? "enabled" : "disabled"));
	}
	
	@CommandHandler(name = "enter")
	public void enter(Player player, String[] args){
		if(!GameMaster.players.get(player).isActive){
			GameMaster.players.put(player, PlayerStatus.PLAYING);
			if(GameMaster.status.isActive)
				GameMaster.activeGame.insertPlayer(player);
			else
				player.teleport(GameMaster.waitingRoom);
			player.setGameMode(GameMode.SURVIVAL);
			TagAPI.refreshPlayer(player);
			player.sendMessage(ChatUtils.NORMAL + "You have entered the game");
		}
	}

	@CommandHandler(name = "exit")
	public void exit(Player player, String[] args){
		if(GameMaster.players.get(player) != PlayerStatus.NOT_PLAYING){
			GameMaster.players.put(player, PlayerStatus.NOT_PLAYING);
			GameMaster.awaitingRespawn.remove(player);
			if(GameMaster.status.isActive)
				GameMaster.activeGame.extractPlayer(player);
			player.setGameMode(GameMode.CREATIVE);
			TagAPI.refreshPlayer(player);
			player.sendMessage(ChatUtils.NORMAL + "You have exited the game");
		}
	}

	@CommandHandler(name = "spectate")
	public void spectate(Player player, String[] args){
		if(GameMaster.players.get(player) != PlayerStatus.SPECTATING){
			GameMaster.players.put(player, PlayerStatus.SPECTATING);
			GameMaster.awaitingRespawn.remove(player);
			if(GameMaster.status.isActive)
				GameMaster.activeGame.extractPlayer(player);
			player.setGameMode(GameMode.CREATIVE);
			TagAPI.refreshPlayer(player);
			player.sendMessage(ChatUtils.NORMAL + "You are now spectating the game");
		}
	}

	@CommandHandler(name = "setwait")
	public void setwait(Player player, String[] args){
		GameMaster.waitingRoom = player.getLocation();
		player.sendMessage(ChatUtils.NORMAL + "The waiting room was set to your location");
	}

	@CommandHandler(name = "endgame")
	public void endgame(CommandSender sender, String[] args){
		if(GameMaster.activeGame != null && GameMaster.activeGame.isActive())
			GameMaster.activeGame.endGame(null);
	}

	@CommandHandler(name = "nextgame")
	public void nextgame(CommandSender sender, String[] args){
		AutoGame event = GameMaster.getRegisteredGame(args[0]);
		if(event == null){
			sender.sendMessage(ChatUtils.ERROR + "Sorry, we couldn't find that game");
			return;
		}
		GameMaster.nextGame = event;
		GameMaster.nextMap = null;
		sender.sendMessage(ChatUtils.NORMAL + "Set the next game to " + ChatUtils.normalEmphasis(event.getGameName()));
	}

	@CommandHandler(name = "nextmap")
	public void nextmap(CommandSender sender, String[] args){
		AutoGame event = GameMaster.nextGame;
		if(event == null){
			sender.sendMessage(ChatUtils.ERROR + "There is no event scheduled yet");
			return;
		}
		if(event.getMap(args[0]) == null){
			sender.sendMessage(ChatUtils.ERROR + "Sorry, we couldn't find that map");
			return;
		}
		GameMaster.nextMap = event.getMap(args[0]);
		sender.sendMessage(ChatUtils.NORMAL + "Set the next map to " + ChatUtils.normalEmphasis(GameMaster.nextMap));
	}
		
	@CommandHandler(name = "patch")
	public void patch(CommandSender sender, String[] args){
		String reason = "";
		for(String str : args)
			reason += str + " ";
		reason = reason.trim();
		for (Player all : Bukkit.getOnlinePlayers()){
			if(reason.equals(""))
				all.kickPlayer("The server is restarting to put in a patch");	
			else
				all.kickPlayer("The server is restarting to patch " + reason);
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
	}
		
	@CommandHandler(name = "gm-debug-cycle")
	public void gmdebugcycle(CommandSender sender, String[] args){	
		GameMaster.DEBUG_CYCLE = true;
		sender.sendMessage(ChatUtils.NORMAL + "Printing debug info for one cycle of recurring ops");
	}

}
