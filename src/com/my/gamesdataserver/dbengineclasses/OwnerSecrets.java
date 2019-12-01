package com.my.gamesdataserver.dbengineclasses;

public class OwnerSecrets {
	private int id;
	private int ownerId;
	private String apiKey;
	private String apiSecret;
	
	public OwnerSecrets(int id, int ownerId, String apiKey, String apiSecret) {
		this.id = id;
		this.ownerId = ownerId;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
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

	public String getApiSecret() {
		return apiSecret;
	}
}
