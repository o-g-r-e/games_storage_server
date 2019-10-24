package com.my.gamesdataserver.defaultgameclasses;

public class Boost {
	private int id;
	private int playerId;
	private String name;
	private int count;
	
	public Boost(int id, int playerId, String name, int count) {
		this.id = id;
		this.playerId = playerId;
		this.name = name;
		this.count = count;
	}

	public int getId() {
		return id;
	}

	public int getPlayerId() {
		return playerId;
	}

	public String getName() {
		return name;
	}

	public int getCount() {
		return count;
	}
}
