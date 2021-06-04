package com.cm.dataserver;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cm.databaseserver.exceptions.AuthorizationException;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.PlayerId;

import io.netty.handler.codec.http.FullHttpRequest;

public class Authorization {
	public static final String PLAYER_ID_HEADER = "Player_id";
	
	static class AuthValue {
		private String requestHash;
		private String gameHash;
		
		public AuthValue(String requestHash, String gameHash) {
			this.requestHash = requestHash;
			this.gameHash = gameHash;
		}

		public String getRequestHash() {
			return requestHash;
		}

		public String getGameHash() {
			return gameHash;
		}
	}
	
	public static boolean computeRequestHash(String uri, String requestHash, String gameHash) throws InvalidKeyException, NoSuchAlgorithmException {
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

	public static boolean checkAuthorizationHeader(FullHttpRequest httpRequest) throws InvalidKeyException, NoSuchAlgorithmException {
		if(!httpRequest.headers().contains("Authorization")) return false;
		String authorization = httpRequest.headers().get("Authorization");
		String gameHash = authorization.substring(authorization.indexOf(":")+1);
		String requestHash = authorization.substring(0, authorization.indexOf(":"));
		return StringDataHelper.validateBase64(gameHash) && StringDataHelper.validateBase64(requestHash) && computeRequestHash(httpRequest.uri(), requestHash, gameHash);
	}
	
	public static AuthValue authorization(FullHttpRequest httpRequest) throws InvalidKeyException, NoSuchAlgorithmException, AuthorizationException {
		if(!checkAuthorizationHeader(httpRequest)) {
			throw new AuthorizationException("Authorization Exception: Authorization error");
		}
		String authorization = httpRequest.headers().get("Authorization");
		String gameHash = authorization.substring(authorization.indexOf(":")+1);
		String requestHash = authorization.substring(0, authorization.indexOf(":"));
		return new AuthValue(requestHash, gameHash);
	}
}
