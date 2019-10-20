package com.my.gamesdataserver.gamesdbmanager;

public class GameEntity {
	private int id;
	private String name;
	private String javaPackage;
	private int ownerId;
	private String key;
	private String tables;
	
	public GameEntity(int id, String name, String javaPackage, int ownerId, String key, String tables) {
		this.id = id;
		this.name = name;
		this.javaPackage = javaPackage;
		this.ownerId = ownerId;
		this.key = key;
		this.tables = tables;
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

	public String getTables() {
		return tables;
	}

	public String getJavaPackage() {
		return javaPackage;
	}
}
