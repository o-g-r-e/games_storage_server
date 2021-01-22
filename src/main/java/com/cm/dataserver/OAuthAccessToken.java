package com.cm.dataserver;

import java.io.Serializable;

public class OAuthAccessToken implements Serializable {
	private Long expirationPeriod;
	private Long generationTimestamp;
	private String accessToken;
	
	private static final OAuthAccessToken INSTANCE = new OAuthAccessToken();
	
	private OAuthAccessToken() {
		generationTimestamp = expirationPeriod = 0L;
	}
	
	public static OAuthAccessToken getInstatnce() {
		return INSTANCE;
	}
	
	/*private OAuthAccessTokenVariables(Long expirationPeriod, Long generationTimestamp) {
		this.expirationPeriod = expirationPeriod;
		this.generationTimestamp = generationTimestamp;
	}
	
	private OAuthAccessTokenVariables(long expirationPeriod, long generationTimestamp) {
		this.expirationPeriod = expirationPeriod;
		this.generationTimestamp = generationTimestamp;
	}*/

	public Long getExpirationPeriod() {
		return expirationPeriod;
	}

	public Long getGenerationTimestamp() {
		return generationTimestamp;
	}
	
	public String getAccessToken() {
		return accessToken;
	}

	public void setExpirationPeriod(Long expirationPeriod) {
		this.expirationPeriod = expirationPeriod;
	}

	public void setGenerationTimestamp(Long generationTimestamp) {
		this.generationTimestamp = generationTimestamp;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public boolean expire() {
		return (System.currentTimeMillis() - getGenerationTimestamp()) > getExpirationPeriod();
	}
}
