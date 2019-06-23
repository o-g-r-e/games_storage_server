package com.my.gamesdataserver;

public class SaveEntity {
	private int id;
	private int gameId;
	private int playerId;
	private int levelNum;
	private int levelStars;
	
	public SaveEntity(int id, int gameId, int playerId, int levelNum, int levelStars) {
		this.id = id;
		this.gameId = gameId;
		this.playerId = playerId;
		this.levelNum = levelNum;
		this.levelStars = levelStars;
	}
	
	public int getId() {
		return id;
	}
	
	public int getGameId() {
		return gameId;
	}
	
	public int getPlayerId() {
		return playerId;
	}
	
	public int getLevelNum() {
		return levelNum;
	}
	
	public int getLevelStars() {
		return levelStars;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",").append(gameId).append(",").append(playerId).append(",").append(levelNum).append(",").append(levelStars).append("]").toString();
	}
}
