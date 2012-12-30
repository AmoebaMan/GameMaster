package com.amoebaman.gamemaster.utils;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.command.CommandSender;

public class ChatUtils {

	public static int SCREEN_WIDTH;
	public static int REG_CHAR_WIDTH;
	private static HashMap<Character, Integer> IRREG_CHAR_WIDTH;
	public static String DESIGN_LEFT, DESIGN_RIGHT, NORMAL, NORMAL_EMPHASIS, HIGHLIGHT, HIGHLIGHT_EMPHASIS, ERROR;
	
	static{
		SCREEN_WIDTH = 316;
		REG_CHAR_WIDTH = 6;
		IRREG_CHAR_WIDTH = new HashMap<Character, Integer>();
		IRREG_CHAR_WIDTH.put(' ', 4);
		IRREG_CHAR_WIDTH.put('i', 2);
		IRREG_CHAR_WIDTH.put('I', 4);
		IRREG_CHAR_WIDTH.put('k', 5);
		IRREG_CHAR_WIDTH.put('l', 3);
		IRREG_CHAR_WIDTH.put('t', 4);
		IRREG_CHAR_WIDTH.put('!', 2);
		IRREG_CHAR_WIDTH.put('(', 5);
		IRREG_CHAR_WIDTH.put(')', 5);
		IRREG_CHAR_WIDTH.put('~', 7);
		IRREG_CHAR_WIDTH.put(',', 2);
		IRREG_CHAR_WIDTH.put('.', 2);
		IRREG_CHAR_WIDTH.put('<', 5);
		IRREG_CHAR_WIDTH.put('>', 5);
		IRREG_CHAR_WIDTH.put(':', 2);
		IRREG_CHAR_WIDTH.put(';', 2);
		IRREG_CHAR_WIDTH.put('"', 5);
		IRREG_CHAR_WIDTH.put('[', 4);
		IRREG_CHAR_WIDTH.put(']', 4);
		IRREG_CHAR_WIDTH.put('{', 5);
		IRREG_CHAR_WIDTH.put('}', 5);
		IRREG_CHAR_WIDTH.put('|', 2);
		IRREG_CHAR_WIDTH.put('`', 0);
		IRREG_CHAR_WIDTH.put('\'', 2);
		IRREG_CHAR_WIDTH.put(ChatColor.COLOR_CHAR, 0);
		
		DESIGN_LEFT = ChatColor.GRAY + " -======" + ChatColor.DARK_GRAY + "|---";
		DESIGN_RIGHT = ChatColor.DARK_GRAY + "---|" + ChatColor.GRAY + "======- ";
		NORMAL = ChatColor.GRAY.toString();
		NORMAL_EMPHASIS = ChatColor.DARK_GRAY.toString();
		HIGHLIGHT = ChatColor.GOLD.toString();
		HIGHLIGHT_EMPHASIS = ChatColor.DARK_RED.toString();
		ERROR = ChatColor.DARK_RED.toString() + ChatColor.ITALIC.toString();
	}
	
	private static int getCharWidth(char value){
		if(IRREG_CHAR_WIDTH.containsKey(value))
			return IRREG_CHAR_WIDTH.get(value);
		return REG_CHAR_WIDTH;
	}
	
	private static int getStringWidth(String str){
		int length = 0;
		for(int i = 0; i < str.length(); i++)
			if(i == 0)
				length += getCharWidth(str.charAt(i));
			else if(str.charAt(i - 1) != ChatColor.COLOR_CHAR)
				length += getCharWidth(str.charAt(i));
		return length;
	}
	
	public static String spacerLine(){
		return ChatColor.RESET.toString();
	}
	
	public static String fillerLine(String pattern){
		float length = getStringWidth(pattern);
		int iterations = (int) (SCREEN_WIDTH / length);
		String line = "";
		for(int i = 0; i < iterations; i++)
			line += pattern;
		return centerAlign(line);
	}

	public static void sendSpacerLine(CommandSender sender){
		sender.sendMessage(spacerLine());
	}
	
	public static void sendFillerLine(CommandSender sender, String pattern){
		sender.sendMessage(fillerLine(pattern));
	}
	
	public static String centerAlign(String text){
		int numSpaces = ((SCREEN_WIDTH - getStringWidth(text)) / 2) / getCharWidth(' ');
		for(int i = 0; i < numSpaces; i++)
			text = " " + text;
		return text;
	}
	
	public static String normalEmphasis(Object text){
		return NORMAL_EMPHASIS + text + NORMAL;
	}
	
	public static String highlightEmphasis(Object text){
		return HIGHLIGHT_EMPHASIS + text + HIGHLIGHT;
	}
	
	public static ChatColor dyeToChatColor(DyeColor color){
		if(color == null) return ChatColor.RESET;
		switch(color){
		case BLACK: return ChatColor.BLACK;
		case BLUE: return ChatColor.BLUE;
		case BROWN: return ChatColor.BOLD;
		case CYAN: return ChatColor.DARK_AQUA;
		case GRAY: return ChatColor.DARK_GRAY;
		case GREEN: return ChatColor.DARK_GREEN;
		case LIGHT_BLUE: return ChatColor.BLUE;
		case LIME: return ChatColor.GREEN;
		case MAGENTA: return ChatColor.LIGHT_PURPLE;
		case ORANGE: return ChatColor.GOLD;
		case PINK: return ChatColor.RED;
		case PURPLE: return ChatColor.DARK_PURPLE;
		case RED: return ChatColor.RED;
		case SILVER: return ChatColor.GRAY;
		case WHITE: return ChatColor.WHITE;
		case YELLOW: return ChatColor.YELLOW;
		default: return ChatColor.RESET;
		}
	}
	
	public static String normalColor(ChatColor color, String text){
		return color + text + NORMAL;
	}
	
	public static String highlightColor(ChatColor color, String text){
		return color + text + HIGHLIGHT;
	}
}
