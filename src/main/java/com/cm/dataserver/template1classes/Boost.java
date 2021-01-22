package com.cm.dataserver.template1classes;

public class Boost {
	private int playerId;
	private String boostName;
	private int count;
	
	public Boost(int playerId, String boostName, int count) {
		this.playerId = playerId;
		this.boostName = boostName;
		this.count = count;
	}

	public int getPlayerId() {
		return playerId;
	}

	public String getBoostName() {
		return boostName;
	}

	public int getCount() {
		return count;
	}
}
