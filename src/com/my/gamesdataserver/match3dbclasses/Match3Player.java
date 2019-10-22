package com.my.gamesdataserver.match3dbclasses;

public class Match3Player {
	private int id;
	private String playerId;
	private int maxLevel;

	public Match3Player(int id, String playerId, int maxLevel) {
		this.id = id;
		this.playerId = playerId;
		this.maxLevel = maxLevel;
	}
	
	public int getId() {
		return id;
	}
	
	public String getPlayerId() {
		return playerId;
	}

	public int getMaxLevel() {
		return maxLevel;
	}
}
