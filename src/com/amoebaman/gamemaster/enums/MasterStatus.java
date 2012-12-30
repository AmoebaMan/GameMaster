package com.amoebaman.gamemaster.enums;

public enum MasterStatus {

	RUNNING(true),
	SUSPENDED(true),
	INTERMISSION(false),
	PREP(false),
	;
	
	public boolean isActive;
	private MasterStatus(boolean active){ this.isActive = active; }
	
}
