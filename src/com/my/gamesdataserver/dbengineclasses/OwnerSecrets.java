package com.my.gamesdataserver.dbengineclasses;

public class OwnerSecrets {
	private int ownerId;
	private String apiKey;
	private String apiSecret;
	
	public OwnerSecrets(int ownerId, String apiKey, String apiSecret) {
		this.ownerId = ownerId;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
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
