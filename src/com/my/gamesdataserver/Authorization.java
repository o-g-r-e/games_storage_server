package com.my.gamesdataserver;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.my.gamesdataserver.dbengineclasses.Game;
import com.my.gamesdataserver.dbengineclasses.GamesDbEngine;

import io.netty.handler.codec.http.FullHttpRequest;

public class Authorization {
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
	
	public boolean requestAuthentication(FullHttpRequest httpRequest) throws InvalidKeyException, NoSuchAlgorithmException {
		String authorization = httpRequest.headers().get("Authorization");
		
		if(authorization == null || "".equals(authorization) || !authorization.contains(":")) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		}
		
		String inputGameHash = authorization.substring(authorization.indexOf(":")+1);
		String inputRequestHash = authorization.substring(0, authorization.indexOf(":"));
		
		boolean auth = checkRequestHash(httpRequest.uri(), inputRequestHash, inputGameHash);
		
		if(!auth) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		}
		
		return true;
	}
	
	public boolean authorization(FullHttpRequest httpRequest, Game game, GamesDbEngine dbManager) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
		
		if(!requestAuthentication(httpRequest)) {
			statusCode = Code.REQUEST_AUTH_FAIL;
			return false;
		} /*if(!gameAuthentication(httpRequest, game, dbManager)) {
			statusCode = Code.GAME_AUTH_FAIL;
			return false;
		}*/
		
		String playerId = httpRequest.headers().get("Player_id");
		
		if(playerId == null || "".equals(playerId) || playerId.contains("%") || playerId.contains("_") || dbManager.getPlayerById(playerId, game.getPrefix()) == null) {
			statusCode = Code.PLAYER_AUTH_FAIL;
			return false;
		}
		
		statusCode = Code.SUCCESS;
		return true;
	}
	
	private boolean checkRequestHash(String uri, String requestHash, String gameHash) throws InvalidKeyException, NoSuchAlgorithmException {
		String key = gameHash;
		String hash = generateHmacHash(key, gameHash.substring(0, 8)+uri);
		return hash.equals(requestHash);
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
