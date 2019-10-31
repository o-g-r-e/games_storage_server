package com.my.gamesdataserver.template1classes;

public class Level {
	private int id;
	private int playerId;
	private int level;
	private int score;
	private int stars;
	
	public Level(int id, int playerId, int level, int score, int stars) {
		this.id = id;
		this.playerId = playerId;
		this.level = level;
		this.score = score;
		this.stars = stars;
	}

	public int getId() {
		return id;
	}

	public int getPlayerId() {
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
