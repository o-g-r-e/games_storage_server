package com.my.gamesdataserver.dbmodels;

public class PlayerEntity {
	private int id;
	private String name;
	private String playerId;
	private int gameId;
	
	public PlayerEntity(int id, String name, String playerId, int gameId) {
		this.id = id;
		this.name = name;
		this.playerId = playerId;
		this.gameId = gameId;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	
	public int getGameId() {
		return gameId;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(name).append("\",\"").append(playerId).append("\"]").toString();
	}
}
