package com.cm.dataserver.dbengineclasses;

import java.util.Set;

public class Game {
	private int id;
	private String name;
	private String type;
	private int ownerId;
	private String apiKey;
	private String secretKey;
	private String prefix;
	//private String playerIdFieldName;
	private String hash;
	
	public Game(int id, String name, String type, int ownerId, String apiKey, String secretKey, String prefix, String hash) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.ownerId = ownerId;
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		this.prefix = prefix;
		this.hash = hash;
	}
	
	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}
	
	public int getOwnerId() {
		return ownerId;
	}
	
	public String getKey() {
		return apiKey;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append("\"").append(name).append("\",").append(ownerId).append(",\"").append(apiKey).append("\"]").toString();
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public String getHash() {
		return hash;
	}

	/*public String getPlayerIdFieldName() {
		return playerIdFieldName;
	}*/
	
	public int getId() {
		return id;
	}
}
