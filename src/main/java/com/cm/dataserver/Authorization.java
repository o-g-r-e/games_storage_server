package com.cm.dataserver;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.PlayerId;

import io.netty.handler.codec.http.FullHttpRequest;

public class Authorization {
	public static final String PLAYER_ID_HEADER = "Player_id";
	private enum Code {SUCCESS, REQUEST_AUTH_FAIL, /*GAME_AUTH_FAIL,*/ PLAYER_AUTH_FAIL, NULL};
	
	private Code statusCode = Code.NULL;
	
	/*public boolean gameAuthentication(FullHttpRequest httpRequest, Game game, GamesDbEngine dbManager) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
		String authorization = httpRequest.headers().get("Authorization");
		if(authorization != null && !"".equals(authorization) && authorization.contains(":")) {
			// hmac:
			
			String inputGameHash = authorization.substring(authorization.indexOf(":")+1);
			
			game = dbManager.getGameByHash(inputGameHash);
			
			if(game != null) {
				return true;
			}
		} else {
			String apiKey = httpRequest.headers().get("API_key");
			
			if(apiKey == null || "".equals(apiKey)) {
				return false;
			}
			
			game = dbManager.getGameByKey(apiKey);
			
			if(game != null) {
				return true;
			}
		}
		
		return false;
	}*/
	
	public boolean checkAuthorizationHeader(FullHttpRequest httpRequest) throws InvalidKeyException, NoSuchAlgorithmException {
		String authorizationString = httpRequest.headers().get("Authorization");
		
		if(authorizationString == null || authorizationString.length() <= 0 || !authorizationString.contains(":")) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		}
		
		String gameHash = authorizationString.substring(authorizationString.indexOf(":")+1);
		String requestHash = authorizationString.substring(0, authorizationString.indexOf(":"));
		
		boolean requestAuthenticationStatus = checkRequestHash(httpRequest.uri(), requestHash, gameHash);
		
		if(!requestAuthenticationStatus) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		}
		
		return true;
	}
	
	private boolean checkPlayerId(String playerId, String gamePrefix, Connection connection) throws SQLException {
		if( playerId == null || 
			playerId.length() <= 0 || 
			playerId.contains("%") || 
			playerId.contains("_") || 
			DataBaseMethods.getPlayerById(new PlayerId("playerId", playerId), gamePrefix, connection) == null) {
			return false;
		}
		return true;
	}
	
	public boolean authorization(FullHttpRequest httpRequest, Game game, Connection connection) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
		
		if(!checkAuthorizationHeader(httpRequest)) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		} 
		
		/*if(!gameAuthentication(httpRequest, game, dbManager)) {
			statusCode = Code.GAME_AUTH_FAIL;
			return false;
		}*/
		
		String playerId = httpRequest.headers().get(Authorization.PLAYER_ID_HEADER);
		
		if(!checkPlayerId(playerId, game.getPrefix(), connection)) {
			statusCode = Code.PLAYER_AUTH_FAIL;
			return false;
		}
		
		statusCode = Code.SUCCESS;
		return true;
	}
	
	private boolean checkRequestHash(String uri, String requestHash, String gameHash) throws InvalidKeyException, NoSuchAlgorithmException {
		String key = gameHash;
		String hmacHash = generateHmacHash(key, gameHash.substring(0, 8)+uri);
		return hmacHash.equals(requestHash);
	}
	
	public static String generateHmacHash(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
		return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes()));
		//return new String(sha256_HMAC.doFinal(data.getBytes()));
	}

	public String getStatusMessage() {
		switch (statusCode) {
		/*case GAME_AUTH_FAIL:
			return "Game authorization fail";*/
		case PLAYER_AUTH_FAIL:
			return "Player authorization fail";
		case REQUEST_AUTH_FAIL:
			return "Request authentication fail";
		case SUCCESS:
			return "Authorization success";
		}
		return "Unknown aouthorization status";
	}
}
