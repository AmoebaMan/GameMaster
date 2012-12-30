package com.amoebaman.gamemaster.enums;

public enum PlayerStatus{

	PLAYING(true),
	RESPAWNING(true),
	SPECTATING(false),
	NOT_PLAYING(false),
	;

	public boolean isActive;
	private PlayerStatus(boolean active){ this.isActive = active; }
}

