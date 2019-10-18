package com.my.gamesdataserver.gamesdbmanager;

public class BoostEntity {
	private int id;
	private int gameId;
	private int playerId;
	private String boostsData;
	
	public BoostEntity(int id, int gameId, int playerId, String boostsData) {
		this.id = id;
		this.gameId = gameId;
		this.playerId = playerId;
		this.boostsData = boostsData;
		this.boostsData = this.boostsData.replace("\"", "\\\"");
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

	public String getBoostsData() {
		return boostsData;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",").append(gameId).append(",").append(playerId).append(",\"").append(boostsData).append("\"]").toString();
	}
}
