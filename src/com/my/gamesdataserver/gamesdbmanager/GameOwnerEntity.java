package com.my.gamesdataserver.gamesdbmanager;

public class GameOwnerEntity {
	private int id;
	private String name;
	
	public GameOwnerEntity(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(name).append("\"]").toString();
	}
}
