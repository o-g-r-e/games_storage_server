package com.cm.dataserver.template1classes;

public class Player {
	private String playerId;
	private String facebookId;

	public Player(String playerId, String facebookId) {
		this.playerId = playerId;
		this.facebookId = facebookId;
	}
	
	public String getPlayerId() {
		return playerId;
	}
}
