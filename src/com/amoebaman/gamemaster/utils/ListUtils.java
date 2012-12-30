package com.amoebaman.gamemaster.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.entity.Player;

import com.amoebaman.gamemaster.objects.UniqueList;
import com.amoebaman.pvpstattracker.StatHandler;

public class ListUtils {

	public static UniqueList<Player> sort(UniqueList<Player> list){
		list = flushOffline(list);
		if(list == null)
			return null;
		Collections.sort(list, new PlayerComparator());
		return list;
	}
	
	public static List<UniqueList<Player>> split(UniqueList<Player> list, int divisions){
		list = flushOffline(list);
		List<UniqueList<Player>> toReturn = new ArrayList<UniqueList<Player>>(divisions);
		if(divisions <= 1){
			toReturn.add(list);
			return toReturn;
		}
		for(int i = 0; i < divisions; i++)
			toReturn.add(new UniqueList<Player>());
		
		divisions--;
		int cycle = 0;
		boolean rising = true;
		for(Player player : list){
			if(rising)
				toReturn.get(cycle).add(player);
			else
				toReturn.get(divisions - cycle).add(player);
			cycle++;
			if(cycle >= divisions){
				cycle = 0;
				rising = !rising;
			}
		}
		
		return toReturn;
	}
	
	public static UniqueList<Player> halfShuffle(UniqueList<Player> list){
		UniqueList<Player> toReturn = new UniqueList<Player>();
		for(int i = 0; i < list.size(); i += 2){
			double random = Math.random();
			if(random > 0.5)
				toReturn.add(list.get(i));
			if(list.size() > i + 1)
				toReturn.add(list.get(i + 1));
			if(random <= 0.5)
				toReturn.add(list.get(i));
		}
		if(toReturn.size() < list.size())
			toReturn.addAll(list);
		return toReturn;
	}
	
	public static int instances(Collection<String> collection, String target){
		int count = 0;
		for(String element : collection)
			if(element.equals(target))
				count++;
		return count;
	}
	
	public static UniqueList<Player> flushOffline(UniqueList<Player> list){
		UniqueList<Player> flushed = new UniqueList<Player>();
		for(Player player : list)
			if(player.isOnline())
				flushed.add(player);
		return flushed;
	}
	
	public static class PlayerComparator implements Comparator<Player>{
		public int compare(Player p1, Player p2) {
			double elo1 = StatHandler.getStat(p1, "elo skill");
			double elo2 = StatHandler.getStat(p2, "elo skill");
			if(elo1 < elo2)
				return -1;
			if(elo1 > elo2)
				return 1;
			return 0;
		}
	}
	
}
