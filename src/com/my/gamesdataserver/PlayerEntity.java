package com.my.gamesdataserver;

public class PlayerEntity {
	private int id;
	private String name;
	private String playerId;
	
	public PlayerEntity(int id, String name, String playerId) {
		this.id = id;
		this.name = name;
		this.playerId = playerId;
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
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(name).append("\",\"").append(playerId).append("\"]").toString();
	}
}
