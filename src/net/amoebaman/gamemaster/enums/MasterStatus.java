package net.amoebaman.gamemaster.enums;

public enum MasterStatus {

	RUNNING(true),
	SUSPENDED(true),
	INTERMISSION(false),
	PREP(false),
	;
	
	public boolean active;
	private MasterStatus(boolean active){ this.active = active; }
	
}
