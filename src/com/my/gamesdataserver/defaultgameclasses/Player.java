package com.my.gamesdataserver.defaultgameclasses;

public class Player {
	private int id;
	private String playerId;
	private int maxLevel;

	public Player(int id, String playerId, int maxLevel) {
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
