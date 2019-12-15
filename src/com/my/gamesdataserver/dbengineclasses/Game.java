package com.my.gamesdataserver.dbengineclasses;

import java.util.Set;

public class Game {
	private String name;
	private String javaPackage;
	private int ownerId;
	private String apiKey;
	private String secretKey;
	private String type;
	private String prefix;
	private String hash;
	
	public Game(String name, String javaPackage, int ownerId, String apiKey, String secretKey, String type, String prefix, String hash) {
		this.name = name;
		this.javaPackage = javaPackage;
		this.ownerId = ownerId;
		this.apiKey = apiKey;
		this.secretKey = secretKey;
		this.type = type;
		this.prefix = prefix;
		this.hash = hash;
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
		return result.append("[").append("\"").append(name).append("\",").append(ownerId).append(",\"").append(apiKey).append("\"]").toString();
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

	public String getSecretKey() {
		return secretKey;
	}

	public String getHash() {
		return hash;
	}
}
