package com.my.gamesdataserver.gamesdbclasses;

import java.util.Set;

public class Game {
	private int id;
	private String name;
	private String javaPackage;
	private int ownerId;
	private String apiKey;
	private String type;
	private String prefix;
	
	public Game(int id, String name, String javaPackage, int ownerId, String apiKey, String type, String prefix) {
		this.id = id;
		this.name = name;
		this.javaPackage = javaPackage;
		this.ownerId = ownerId;
		this.apiKey = apiKey;
		this.type = type;
		this.prefix = prefix;
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
		return apiKey;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(name).append("\",").append(ownerId).append(",\"").append(apiKey).append("\"]").toString();
	}

	public String getJavaPackage() {
		return javaPackage;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getType() {
		return type;
	}

	public String getPrefix() {
		return prefix;
	}
}
