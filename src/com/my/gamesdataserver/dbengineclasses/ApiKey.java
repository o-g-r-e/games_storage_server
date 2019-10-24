package com.my.gamesdataserver.dbengineclasses;

public class ApiKey {
	private int id;
	private int ownerId;
	private String apiKey;
	
	public ApiKey(int id, int ownerId, String apiKey) {
		this.id = id;
		this.ownerId = ownerId;
		this.apiKey = apiKey;
	}

	public int getId() {
		return id;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public String getApiKey() {
		return apiKey;
	}
}
