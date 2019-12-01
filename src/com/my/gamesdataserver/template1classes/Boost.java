package com.my.gamesdataserver.template1classes;

public class Boost {
	private int id;
	private String playerId;
	private int boost1count;
	private int boost2count;
	private int boost3count;
	private int boost4count;
	private int boost5count;
	
	public Boost(int id, String playerId, int boost1count, int boost2count, int boost3count, int boost4count,int boost5count) {
		this.id = id;
		this.playerId = playerId;
		this.boost1count = boost1count;
		this.boost2count = boost2count;
		this.boost3count = boost3count;
		this.boost4count = boost4count;
		this.boost5count = boost5count;
	}

	public int getId() {
		return id;
	}

	public String getPlayerId() {
		return playerId;
	}

	public int getBoost1count() {
		return boost1count;
	}

	public int getBoost2count() {
		return boost2count;
	}

	public int getBoost3count() {
		return boost3count;
	}

	public int getBoost4count() {
		return boost4count;
	}

	public int getBoost5count() {
		return boost5count;
	}
}
