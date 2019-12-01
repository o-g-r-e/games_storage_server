package com.my.gamesdataserver.template1classes;

public class Player {
	private int id;
	private String facebookId;
	private String playerId;
	private int maxLevel;

	public Player(int id, String facebookId, String playerId, int maxLevel) {
		this.id = id;
		this.playerId = playerId;
		this.facebookId = facebookId;
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
