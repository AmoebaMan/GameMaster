package net.amoebaman.gamemaster.enums;

import java.util.Random;

public enum Time {
	
	DAWN(22000),
	DAY(6000),
	DUSK(14000),
	NIGHT(18000),
	RANDOM(0),
	;
	
	public int ticks;
	private Time(int ticks){ this.ticks = ticks; }
	
	public static Time getRandom(){
		return values()[new Random().nextInt(values().length - 1)];
	}
	
	public static Time matchString(String str){
		return valueOf(str.toUpperCase().replace(' ', '_'));
	}
	
}
