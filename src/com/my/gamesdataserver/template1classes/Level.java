package com.my.gamesdataserver.template1classes;

public class Level {
	private int id;
	private String playerId;
	private int level;
	private int score;
	private int stars;
	
	public Level(int id, String playerId, int level, int score, int stars) {
		this.id = id;
		this.playerId = playerId;
		this.level = level;
		this.score = score;
		this.stars = stars;
	}
	
	public Level(String playerId, int level, int score, int stars) {
		this.playerId = playerId;
		this.level = level;
		this.score = score;
		this.stars = stars;
	}

	public int getId() {
		return id;
	}

	public String getPlayerId() {
		return playerId;
	}

	public int getLevel() {
		return level;
	}

	public int getScore() {
		return score;
	}

	public int getStars() {
		return stars;
	}
}
