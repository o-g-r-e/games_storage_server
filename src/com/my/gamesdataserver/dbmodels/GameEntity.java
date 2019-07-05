package com.my.gamesdataserver.dbmodels;

public class GameEntity {
	private int id;
	private String name;
	private int ownerId;
	private String key;
	
	public GameEntity(int id, String name, int ownerId, String key) {
		this.id = id;
		this.name = name;
		this.ownerId = ownerId;
		this.key = key;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getOwnerId() {
		return ownerId;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(name).append("\",").append(ownerId).append(",\"").append(key).append("\"]").toString();
	}
}
