package com.my.gamesdataserver.template1classes;

public class Player {
	private String playerId;
	private String facebookId;
	private int maxLevel;

	public Player(String playerId, String facebookId, int maxLevel) {
		this.playerId = playerId;
		this.facebookId = facebookId;
		this.maxLevel = maxLevel;
	}
	
	public String getPlayerId() {
		return playerId;
	}

	public int getMaxLevel() {
		return maxLevel;
	}
}
