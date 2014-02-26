package net.amoebaman.gamemaster.utils;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;


import org.bukkit.*;
import org.bukkit.command.*;

import com.google.common.collect.Lists;

public class CommandController implements CommandExecutor{
	
	private final static CommandController INSTANCE = new CommandController();
	private final static Set<AnnotatedPluginCommand> commands = new HashSet<AnnotatedPluginCommand>();
	
	private CommandController(){}
	
	/**
	 * Registers all command handlers in a class, matching them with their corresponding commands registered to the specified plugin.
	 * @param plugin The plugin whose commands will be considered for registration
	 * @param handler An instance of the class whose methods will be considered for registration
	 */
	public static void registerCommands(Object handler){
		for(Method method : handler.getClass().getMethods())
			if(method.isAnnotationPresent(CommandHandler.class)){
				Class<?>[] params = method.getParameterTypes();
				if(params.length == 2 && CommandSender.class.isAssignableFrom(params[0]) && String[].class.equals(params[1]))
					new AnnotatedPluginCommand(handler, method);
			}
	}
	
	/**
	 * @author AmoebaMan
	 * An annotation interface that may be attached to a method to designate it as a command handler.
	 * When registering a handler with this class, only methods marked with this annotation will be considered for command registration.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface CommandHandler {
		String cmd();
		String[] aliases() default {};
		String[] permissions() default {};
		String permissionMessage() default "You do not have permission to use that command";
	}
	
	private static class AnnotatedPluginCommand{
		
		public final Object instance;
		public final Method method;
		public final Set<String[]> identifiers;
		public final String[] permissions;
		public final String permissionsMessage;
		
		/**
		 * Constructs an AnnotatedPluginCommand containing all the information necessary to
		 * run the command via the CommandController.  This will also pull all information
		 * specified in the CommandHandler interface and assign it to the actual plugin command
		 * at the root of this AnnotatedPluginCommand.
		 * @param instance
		 * @param method
		 */
		public AnnotatedPluginCommand(Object instance, Method method){
			this.instance = instance;
			this.method = method;
			if(instance == null || method == null)
				throw new IllegalArgumentException("instance and method must not be null");
			if(!Lists.newArrayList(instance.getClass().getMethods()).contains(method))
				throw new IllegalArgumentException("instance and method must be part of the same class");
			
			CommandHandler annot = method.getAnnotation(CommandHandler.class);
			if(annot == null)
				throw new IllegalArgumentException("command method must be annotated with @CommandHandler");
			
			identifiers = new HashSet<String[]>();
			if(annot.cmd().trim().isEmpty())
				throw new IllegalArgumentException("command method must have at least one valid base command");
			identifiers.add(annot.cmd().split(" "));
			for(String alias : annot.aliases())
				if(!alias.trim().isEmpty())
					identifiers.add(alias.split(" "));
			
			Set<String[]> invalid = new HashSet<String[]>();
			for(String[] id : identifiers){
				PluginCommand cmd = Bukkit.getPluginCommand(id[0]);
				if(cmd == null){
					invalid.add(id);
					Logger.getLogger("minecraft").warning("[CommandController] Unable to register command with root identifier (or alias) " + id[0] + ": no Bukkit command is registered with that name");
				}
				else{
					Bukkit.getPluginCommand(id[0]).setExecutor(INSTANCE);
				}
			}
			identifiers.removeAll(invalid);
			
			permissions = annot.permissions();
			permissionsMessage = annot.permissionMessage();
			
			if(!identifiers.isEmpty())
				commands.add(this);
		}
		
		/**
		 * Gets the class of the type of sender that this command needs to be passed to function.
		 * This is determined when the command is registered, by the first parameter type of the method.
		 * Any class assignable from this class can be used to run the command, so using a
		 * <code>CommandSender</code> as the first parameter would allow <i>any</i> sender to run
		 * the command.
		 * @return the necessary sender class
		 */
		public Class<?> getSenderType(){
			return method.getParameterTypes()[0];
		}
		
	}
	
	/**
	 * This is the method that "officially" processes commands, but in reality it will always delegate responsibility to the handlers and methods assigned to the command or subcommand
	 * Beyond checking permissions, checking player/console sending, and invoking handlers and methods, this method does not actually act on the commands
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		
		/*
		 * For all registered commands...
		 */
		cmds: for(AnnotatedPluginCommand cmd : commands)
			/*
			 * For every identifier for the command...
			 */
			for(String[] id : cmd.identifiers)
				/*
				 * If the root command matches...
				 */
				if(id[0].equalsIgnoreCase(command.getName())){
					/*
					 * Make sure we even have enough args to make the match
					 */
					if(args.length < id.length - 1)
						continue cmds;
					/*
					 * Make sure we've matched all required subcmds as well
					 */
					for(int i = 1; i < id.length; i++)
						if(!id[i].equals(args[i - 1]))
							continue cmds;
					/*
					 * Verify that the correct sender was used
					 */
					if(!cmd.getSenderType().isAssignableFrom(sender.getClass())){
						sender.sendMessage(ChatColor.RED + "This command must be sent by a " + cmd.getSenderType().getCanonicalName());
						return true;
					}
					/*
					 * Make sure the sender has permissions
					 */
					for(String node : cmd.permissions)
						if(!sender.hasPermission(node)){
							sender.sendMessage(cmd.permissionsMessage);
							return true;
						}
					/*
					 * Trim down the args and try to process the command
					 */
					String[] newArgs = new String[args.length - (id.length - 1)];
					for(int i = 0; i < newArgs.length; i++)
						newArgs[i] = args[i + (id.length - 1)];
					try {
						cmd.method.invoke(cmd.instance, sender, newArgs);
						return true;
					}
					catch (Exception e) {
						sender.sendMessage(ChatColor.RED + "An error occurred while trying to process the command");
						e.printStackTrace();
					}
				}
	
	return false;
	
	}
	
}