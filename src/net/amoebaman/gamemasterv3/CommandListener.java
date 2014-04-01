package net.amoebaman.gamemasterv3;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import net.amoebaman.gamemasterv3.api.AutoGame;
import net.amoebaman.gamemasterv3.api.GameMap;
import net.amoebaman.gamemasterv3.api.TeamAutoGame;
import net.amoebaman.gamemasterv3.enums.GameState;
import net.amoebaman.gamemasterv3.enums.PlayerState;
import net.amoebaman.gamemasterv3.enums.Team;
import net.amoebaman.gamemasterv3.softdepend.Depend;
import net.amoebaman.gamemasterv3.softdepend.HerochatHandler;
import net.amoebaman.gamemasterv3.util.PropertySet;
import net.amoebaman.kitmaster.Actions;
import net.amoebaman.kitmaster.controllers.ItemController;
import net.amoebaman.kitmaster.enums.Attribute;
import net.amoebaman.kitmaster.handlers.HistoryHandler;
import net.amoebaman.kitmaster.handlers.KitHandler;
import net.amoebaman.kitmaster.objects.Kit;
import net.amoebaman.utils.CommandController.CommandHandler;
import net.amoebaman.utils.GenUtil;
import net.amoebaman.utils.chat.Align;
import net.amoebaman.utils.chat.Chat;
import net.amoebaman.utils.chat.Message;
import net.amoebaman.utils.chat.Scheme;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.util.com.google.common.collect.Lists;

public class CommandListener{
	
	private GameMaster master;
	
	protected CommandListener(GameMaster master){
		this.master = master;
	}
	
	@CommandHandler(cmd = "game")
	public Object gameCmd(CommandSender sender, String[] args){
		master.sendStatus(sender);
		return null;
	}
	
	@CommandHandler(cmd = "vote")
	public Object voteCmd(Player player, String[] args){
		String vote = GenUtil.concat(Lists.newArrayList(args), "", " ", "");
		if(master.getState() == GameState.INTERMISSION){
			if(master.getActiveGame() == null){
				/*
				 * Get the game they voted for
				 */
				AutoGame game = !vote.isEmpty() ? master.getGame(vote) : null;
				/*
				 * List the available games if they haven't chosen one
				 */
				if(game == null){
					List<Message> list = Lists.newArrayList(new Message(Scheme.NORMAL).then("Click to vote for a game:"));
					for(AutoGame each : master.getGames())
						list.add(new Message(Scheme.NORMAL).then(" > ").then(each).strong().tooltip(Chat.format("&xClick to vote for &z" + each, Scheme.NORMAL)).command("/vote " + each.getName()));
					return list;
				}
				/*
				 * Don't allow votes for recently-played games
				 */
				if(master.getProgression().getGameHistory().contains(game))
					return new Message(Scheme.ERROR).then(game).strong().then(" has played recently, choose a different game");
				/*
				 * Log the vote
				 */
				master.logVote(player, game.getName());
				return new Message(Scheme.NORMAL).then("You voted for ").then(game).strong().then(" for the next game");
			}
			else if(master.getActiveMap() == null){
				/*
				 * Get the map they voted for
				 */
				GameMap map = !vote.isEmpty() ? master.getMap(vote) : null;
				/*
				 * List the available maps if they haven't chosen one
				 */
				if(map == null){
					List<Message> list = Lists.newArrayList(new Message(Scheme.NORMAL).then("Click to vote for a map:"));
					for(GameMap each : master.getMaps(master.getActiveGame()))
						list.add(new Message(Scheme.NORMAL).then(" > ").then(each).strong().tooltip(Chat.format("&xClick to vote for &z" + each, Scheme.NORMAL)).command("/vote " + each.getName()));
					return list;
				}
				/*
				 * Don't allow votes for non-compatible maps
				 */
				if(!master.getActiveGame().isCompatible(map))
					return new Message(Scheme.ERROR).then(master.getActiveGame()).strong().then(" can't be played on ").then(map).strong();
				/*
				 * Don't allow votes for recently-played maps
				 */
				if(master.getProgression().getMapHistory().contains(map))
					return new Message(Scheme.ERROR).then(map).strong().then(" has played recently, choose a different game");
				/*
				 * Log the vote
				 */
				master.logVote(player, map.getName());
				return new Message(Scheme.NORMAL).then("You voted for ").then(map).strong().then(" for the next map");
			}
			else
				return new Message(Scheme.ERROR).then("The next game and map have already been chosen");
		}
		else
			return new Message(Scheme.ERROR).then("You can only vote on games and maps during the intermission");
	}
	
	private Kit getChargedKit(Kit normal){
		return KitHandler.getKitByIdentifier("C-" + normal.name);
	}
	
	@CommandHandler(cmd = "charge")
	public Object chargesCmd(CommandSender sender, String[] args){
		if(!Depend.hasEconomy() || !Depend.hasKitMaster())
			return new Message(Scheme.ERROR)
				.t("This feature is not available");
		Economy econ = Depend.getEconomy();
		return Align.addSpacers("", Lists.newArrayList(
			new Message(Scheme.HIGHLIGHT)
				.then("You have ")
				.then(econ.getBalance(sender.getName())).strong()
				.then(" " + econ.currencyNamePlural()),
			new Message(Scheme.HIGHLIGHT)
				.then("It costs ")
				.then(master.getConfig().getDouble("currency.cost-to-charge")).s()
				.then(" " + econ.currencyNamePlural() + " to charge a kit"),
				/* 
				 * TODO get rid of hardcoded links 
				 */
			new Message(Scheme.HIGHLIGHT)
				.then("  Click here to vote on PlanetMinecraft")
					.link("http://bit.ly/landwarvotepmc"),
			new Message(Scheme.HIGHLIGHT)
				.then("  Click here to vote on MineStatus")
					.link("http://bit.ly/landwarvotems"),
			new Message(Scheme.HIGHLIGHT)
				.then("  Check here to vote on MinecraftServerList")
					.link("http://bit.ly/landwarvotemcsl"),
			new Message(Scheme.HIGHLIGHT)
				.then("Use charges to power up your kits with ")
				.then("/charges use").strong()
					.tooltip(Scheme.NORMAL.normal + "Click here to use a charge")
					.command("/charges use"),
			new Message(Scheme.HIGHLIGHT)
				.then("Get info about a charged kit with ")
				.then("/charges info <kit>").strong()
					.suggest("/charges info <kit>")
		));
	}
	
	@CommandHandler(cmd = "charge use")
	public Object chargesUseCmd(Player player, String[] args){
		/*
		 * Make sure we've got the required dependencies
		 */
		if(!Depend.hasEconomy() || !Depend.hasKitMaster())
			return new Message(Scheme.ERROR)
				.t("This feature is not available");
		Economy econ = Depend.getEconomy();
		/*
		 * Make sure they've got enough dough
		 */
		if(!econ.has(player.getName(), master.getConfig().getDouble("currency.cost-to-charge")))
			return new Message(Scheme.ERROR).then("You don't have enough " + econ.currencyNamePlural());
		/*
		 * Can't charge up if you haven't taken a kit
		 */
		List<Kit> last = HistoryHandler.getHistory(player);
		if(last == null || last.isEmpty())
			return new Message(Scheme.ERROR).then("You haven't taken a kit to charge");
		/*
		 * Get the charged up version of their most recent kit
		 */
		Kit recent = null;
		for(Kit kit : last)
			if(!kit.stringAttribute(Attribute.IDENTIFIER).contains("parent") && !kit.stringAttribute(Attribute.IDENTIFIER).contains("supplydrop"))
				recent = kit;
		Kit charged = getChargedKit(recent);
		/*
		 * Make sure we've got a valid state to charge up to
		 */
		if(charged == null)
			return new Message(Scheme.ERROR).then("Your kit doesn't have a charged state available");
		/*
		 * Subtract the currency and give them the charged kit
		 */
		econ.withdrawPlayer(player.getName(), master.getConfig().getDouble("currency.cost-to-charge"));
		Actions.giveKit(player, charged, true);
		return new Message(Scheme.NORMAL).then("You charged ")
				.t(recent.name)
				.t(" up to ")
				.t(charged.name);
	}
	
	@CommandHandler(cmd = "charges info")
	public Object chargesInfoCmd(Player sender, String[] args){
		/*
		 * Make sure we've got the required dependencies
		 */
		if(!Depend.hasEconomy() || !Depend.hasKitMaster())
			return new Message(Scheme.ERROR)
				.t("This feature is not available");
		/*
		 * Make sure they've specified a kit and that it's got a charged state
		 */
		if(args.length < 1)
			return new Message(Scheme.ERROR).then("Name a kit to get info about its charged state");
		Kit charged = getChargedKit(KitHandler.getKit(args[0]));
		if(charged == null)
			return new Message(Scheme.ERROR).then("That kit doesn't have an upgraded state available");
		/*
		 * Print the info
		 */
		new Message(Scheme.NORMAL)
			.then("Kit info for ")
			.then(charged.name).strong()
			.send(sender);
		new Message(Scheme.NORMAL)
			.then("Items:")
			.send(sender);
		for(ItemStack item : charged.items)
			new Message(Scheme.NORMAL)
			.then(" - " + ItemController.friendlyItemString(item))
				.itemTooltip(item)
			.send(sender);
		new Message(Scheme.NORMAL)
			.then("Effects:")
			.send(sender);
		for(PotionEffect effect : charged.effects)
			new Message(Scheme.NORMAL)
				.then(" - " + ItemController.friendlyEffectString(effect))
				.send(sender);
		return null;
	}
	
	@CommandHandler(cmd = "teamchat")
	public Object teamchatCmd(Player player, String[] args){
		if(Depend.hasHerochat())
			return new Message(Scheme.NORMAL)
				.then("Team-exclusive chatting is ")
				.then(master.getHerochatHandler().toggleTeamChat(player) ? "enabled" : "disabled").strong();
		else
			return new Message(Scheme.ERROR)
				.then("Team chat is not available");
	}
	
	@CommandHandler(cmd = "fixme")
	public Object fixmeCmd(Player player, String[] args){
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tp " + player.getName() + " " + player.getName());
		return new Message(Scheme.NORMAL).then("No problem");
	}
	
	@CommandHandler(cmd = "changeteam")
	public Object changeteamCmd(CommandSender sender, String[] args){
		if(master.getState() == GameState.INTERMISSION)
			return new Message(Scheme.ERROR).t("There isn't a game running");
		if(!(master.getActiveGame() instanceof TeamAutoGame))
			return new Message(Scheme.ERROR).t("This isn't a team game");
		if(args.length == 0 && !(sender instanceof Player))
			return new Message(Scheme.ERROR).t("Include a player to swap");
		Player target = args.length == 0 ? (Player) sender : Bukkit.getPlayer(args[0]);
		if(target == null)
			return new Message(Scheme.ERROR).t("Couldn't find specified player");
		if(sender.hasPermission("gamemaster.admin")){
			((TeamAutoGame) master.getActiveGame()).changeTeam(target);
			return new Message(Scheme.NORMAL).t("Changing ").t(target.equals(sender) ? "your" : target.getName() + "'s").s().t(" team");
		}
		return null;
	}
	
	@CommandHandler(cmd = "balanceteams")
	public Object balanceteamsCmd(CommandSender sender, String[] args){
		TeamAutoGame.setBalancing(!TeamAutoGame.isBalancing());
		return new Message(Scheme.NORMAL).t("Automatic team balancing is ").t(TeamAutoGame.isBalancing() ? "enabled" : "disabled").s();
	}
	
	@CommandHandler(cmd = "join")
	public Object joinCmd(Player player, String[] args){
		if(master.getState(player) != PlayerState.PLAYING){
			master.setState(player, PlayerState.PLAYING);
			return new Message(Scheme.NORMAL).t("You have joined the games");
		}
		return new Message(Scheme.NORMAL).t("You're already in the games");
	}
	
	@CommandHandler(cmd = "watch")
	public Object watchCmd(Player player, String[] args){
		if(master.getState(player) != PlayerState.WATCHING){
			master.setState(player, PlayerState.WATCHING);
			return new Message(Scheme.HIGHLIGHT).t("You are watching the games");
		}
		return new Message(Scheme.NORMAL).t("You're already watching the games");
	}
	
	@CommandHandler(cmd = "leave")
	public Object leaveCmd(Player player, String[] args){
		if(master.getState(player) != PlayerState.EXTERIOR){
			master.setState(player, PlayerState.EXTERIOR);
			return new Message(Scheme.NORMAL).t("You have left the games");
		}
		return new Message(Scheme.NORMAL).t("You weren't in the games");
	}
	
	@CommandHandler(cmd = "setlobby")
	public Object setWaitCmd(Player player, String[] args){
		master.setLobby(player.getLocation());
		return new Message(Scheme.NORMAL).t("Set the waiting room to your location");
	}
	
	@CommandHandler(cmd = "setfireworks")
	public Object setFireworksCmd(Player player, String[] args){
		master.setFireworks(player.getLocation());
		return new Message(Scheme.NORMAL).t("Set the fireworks launch site to your location");
	}
	
	@CommandHandler(cmd = "setwelcome")
	public Object setWelcoemCmd(Player player, String[] args){
		master.setWelcome(player.getLocation());
		return new Message(Scheme.NORMAL).t("Set the welcome spawn to your location");
	}
	
	@CommandHandler(cmd = "endgame")
	public Object endGameCmd(CommandSender sender, String[] args){
		if(master.getState() != GameState.INTERMISSION){
			master.getActiveGame().abort();
			master.endGame();
		}
		return null;
	}
	
	@CommandHandler(cmd = "nextgame")
	public Object nextGameCmd(CommandSender sender, String[] args){
		AutoGame game = master.getGame(args[0]);
		if(game == null)
			return new Message(Scheme.ERROR).t("That game doesn't exist");
		master.getProgression().setNextGame(game);
		master.getProgression().setNextMap(null);
		return new Message(Scheme.NORMAL).t("Set the next game to ").t(game).s();
	}
	
	@CommandHandler(cmd = "nextmap")
	public Object nextMapCmd(CommandSender sender, String[] args){
		AutoGame game = master.getProgression().getNextGame() == null ? master.getActiveGame() : master.getProgression().getNextGame();
		if(game == null)
			return new Message(Scheme.ERROR).t("There isn't a game scheduled yet");
		GameMap map = master.getMap(args[0]);
		if(map == null)
			return new Message(Scheme.ERROR).t("That map doesn't exist");
		if(!game.isCompatible(map))
			return new Message(Scheme.ERROR).t(game).s().t(" can't be played on ").t(map).s();
		master.getProgression().setNextMap(map);
		return new Message(Scheme.NORMAL).t("Set the next map to ").t(map).s();
	}
	
	@CommandHandler(cmd = "patch")
	public Object patchCmd(CommandSender sender, String[] args){
		String reason = "";
		for(String str : args)
			reason += str + " ";
		reason = reason.trim();
		for(Player all : Bukkit.getOnlinePlayers()){
			if(reason.equals(""))
				all.kickPlayer("The server is restarting to put in a patch");
			else
				all.kickPlayer("The server is restarting to patch " + reason);
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
		return null;
	}
	
	@CommandHandler(cmd = "gm-debug-cycle")
	public Object gmDebugCycleCmd(CommandSender sender, String[] args){
		master.getTicker().debug();
		return new Message(Scheme.NORMAL).then("Priting debug info for one tick");
	}
	
	@CommandHandler(cmd = "game-map create")
	public Object mapCreateCmd(CommandSender sender, String[] args){
		String name = GenUtil.concat(Lists.newArrayList(args), "", " ", "");
		if(name.isEmpty())
			return new Message(Scheme.ERROR).t("Include the name of the new map");
		if(master.getEditMap() != null)
			Bukkit.dispatchCommand(sender, "game-map save");
		if(master.getMap(name) != null)
			return new Message(Scheme.ERROR).t("A map with that name already exists");
		GameMap map = new GameMap(name);
		master.setEditMap(map);
		return new Message(Scheme.NORMAL).t("Created a new map named ").t(name).s();
	}
	
	@CommandHandler(cmd = "game-map edit")
	public Object mapEditCmd(CommandSender sender, String[] args){
		String name = GenUtil.concat(Lists.newArrayList(args), "", " ", "");
		if(name.isEmpty())
			return new Message(Scheme.ERROR).t("Include the name of the map to edit");
		if(master.getEditMap() != null)
			Bukkit.dispatchCommand(sender, "game-map save");
		GameMap map = master.getMap(name);
		if(map == null)
			return new Message(Scheme.ERROR).t("That map doesn't exist");
		master.setEditMap(map);
		return new Message(Scheme.NORMAL).t("Editing the map ").t(master.getEditMap()).s();
	}
	
	@CommandHandler(cmd = "game-map delete")
	public Object mapDeleteCmd(CommandSender sender, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		master.deregisterMap(master.getEditMap());
		GameMap t = master.getEditMap();
		master.setEditMap(null);
		return new Message(Scheme.NORMAL).t("Deleted the map named ").t(t).s();
	}
	
	@CommandHandler(cmd = "game-map save")
	public Object mapSaveCmd(CommandSender sender, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		master.registerMap(master.getEditMap());
		GameMap t = master.getEditMap();
		master.setEditMap(null);
		return new Message(Scheme.NORMAL).t("Saved the map named ").t(t).s();
	}
	
	@CommandHandler(cmd = "game-map info")
	public Object mapInfoCmd(CommandSender sender, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		List<Object> list = new ArrayList<Object>();
		list.add(new Message(Scheme.NORMAL).t("Name: ").s().t(master.getEditMap()));
		PropertySet prop = master.getEditMap().getProperties();
		for(String key : prop.getKeys(false))
			if(prop.isConfigurationSection(key)){
				list.add(new Message(Scheme.NORMAL).t(key).s());
				ConfigurationSection sec = prop.getConfigurationSection(key);
				for(String subKey : sec.getKeys(true))
					if(!sec.isConfigurationSection(subKey))
						list.add(new Message(Scheme.NORMAL).t(subKey + ": ").s().t(sec.get(subKey)));
			}
			else
				list.add(new Message(Scheme.NORMAL).t(key).s().t(prop.get(key)));
		return list;
	}
	
	@CommandHandler(cmd = "game-map list")
	public Object mapListCmd(CommandSender sender, String[] args){
		new Message(Scheme.NORMAL).t("Maps: ").s().t(Chat.format(GenUtil.concat(GenUtil.objectsToStrings(master.getMaps()), "&x", "&z, &x", ""), Scheme.NORMAL)).send(sender);
		if(master.getEditMap() != null)
			new Message(Scheme.NORMAL).t("Editing: ").s().t(master.getEditMap()).send(sender);
		if(args.length > 0){
			AutoGame game = master.getGame(args[0]);
			if(game != null)
				new Message(Scheme.NORMAL).t("Compatible with ").t(game).s().t(": " + Chat.format(GenUtil.concat(GenUtil.objectsToStrings(master.getMaps(game)), "&x", "&z, &x", ""), Scheme.NORMAL)).send(sender);
		}
		return null;
	}
	
	@CommandHandler(cmd = "game-map world")
	public Object mapWorldCmd(Player player, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		master.getEditMap().getProperties().set("world", player.getWorld());
		return new Message(Scheme.NORMAL).t("Map world set to ").t(player.getWorld().getName()).s();
	}
	
	@CommandHandler(cmd = "game-map addteam")
	public Object mapAddTeamCmd(CommandSender sender, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		if(args.length < 1)
			return new Message(Scheme.ERROR).t("Include a team to add");
		Team newTeam = Team.getByString(args[0]);
		if(newTeam == null)
			return new Message(Scheme.ERROR).t("Invalid team...choose from: ").t(Chat.format(GenUtil.concat(Lists.newArrayList(Team.values()), "&z", "&x, &z", ""), Scheme.WARNING));
		List<String> teams = master.getEditMap().getProperties().getStringList("active-teams");
		for(String team : teams)
			if(Team.getByString(team) == newTeam)
				return new Message(Scheme.NORMAL).t("The ").t(newTeam).s().t(" team is already on this map");
		teams.add(newTeam.name());
		master.getEditMap().getProperties().set("active-teams", teams);
		return new Message(Scheme.NORMAL).t("The ").t(newTeam).s().t(" now plays on this map");
	}
	
	@CommandHandler(cmd = "game-map removeteam")
	public Object mapRemoveTeamCmd(CommandSender sender, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		if(args.length < 1)
			return new Message(Scheme.ERROR).t("Include a team to remove");
		Team oldTeam = Team.getByString(args[0]);
		if(oldTeam == null)
			return new Message(Scheme.ERROR).t("Invalid team...choose from: ").t(Chat.format(GenUtil.concat(Lists.newArrayList(Team.values()), "&z", "&x, &z", ""), Scheme.WARNING));
		List<String> teams = master.getEditMap().getProperties().getStringList("active-teams");
		boolean success = teams.remove(oldTeam.name()) || teams.remove(oldTeam.name().toLowerCase());
		master.getEditMap().getProperties().set("active-teams", teams);
		if(success)
			return new Message(Scheme.NORMAL).t("The ").t(oldTeam).s().t(" team has been removed from this map");
		else
			return new Message(Scheme.NORMAL).t("The ").t(oldTeam).s().t(" team was not part of this map");
	}
	
	@CommandHandler(cmd = "game-map setspawn")
	public Object mapSetSpawnCmd(Player player, String[] args){
		if(master.getEditMap() == null)
			return new Message(Scheme.ERROR).t("No map is being edited");
		if(args.length < 1)
			return new Message(Scheme.ERROR).t("Include the team to set the spawn of");
		Team team = Team.getByString(args[0]);
		if(team == null)
			return new Message(Scheme.ERROR).t("Invalid team...choose from: ").t(Chat.format(GenUtil.concat(Lists.newArrayList(Team.values()), "&z", "&x, &z", ""), Scheme.WARNING));
		Location loc = player.getLocation();
		loc.setX(loc.getBlockX() + 0.5);
		loc.setY(loc.getBlockY() + 0.5);
		loc.setZ(loc.getBlockZ() + 0.5);
		master.getEditMap().getProperties().set("team-respawn/" + team.name(), loc);
		return new Message(Scheme.NORMAL).t("Set the ").t(team).s().t(" team's spawn location to your position");
	}
}
