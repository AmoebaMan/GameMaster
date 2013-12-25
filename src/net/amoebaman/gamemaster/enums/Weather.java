package net.amoebaman.gamemaster.enums;

import java.util.Random;

public enum Weather {
	
	CLEAR,
	RAINING,
	STORMING,
	RANDOM,
	;
	
	public static Weather getRandom(){
		return values()[new Random().nextInt(values().length - 1)];
	}
	
	public static Weather matchString(String str){
		return valueOf(str.toUpperCase().replace(' ', '_'));
	}
	
}
