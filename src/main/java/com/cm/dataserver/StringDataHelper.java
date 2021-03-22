package com.cm.dataserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.QueryStringDecoder;

public class StringDataHelper {
	
	private static Pattern specialRequestNamePattern = Pattern.compile("^\\/\\w+\\/(\\w+)(\\?|\\/)?");
	
	
	private static Pattern facebookIdPattern = Pattern.compile("^\\d{8,}$");
	private static Pattern uuidPattern = Pattern.compile("^[a-z0-9]{32}$");
	
	private static Pattern gameNamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\s]+$");
	private static Pattern emailPattern = Pattern.compile("^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$");
	private static Pattern yesNoPattern = Pattern.compile("^(yes|Yes|no|No)$");
	private static Pattern playerIdPattern = Pattern.compile("^[a-z0-9]{8}-[a-z0-9]{8}$");
	private static Pattern base64pattern = Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
	
	public static String parseSpecialRequestName(String urlPath) {
		Matcher requestNameMatcher = specialRequestNamePattern.matcher(urlPath);
		if(requestNameMatcher.find()) return requestNameMatcher.group(1);
		return null;
	}
	
	public static boolean validateFacebookId(String facebookId) {
		return facebookIdPattern.matcher(facebookId).find();
	}
	
	public static boolean validateBase64(String base64string) {
		return base64pattern.matcher(base64string).find();
	}
	
	public static boolean validateUUID(String uuid) {
		return uuidPattern.matcher(uuid).find();
	}
	
	public static boolean validateGameCreationParameters(String gameName, String email, String isMathc3) {
		return gameNamePattern.matcher(gameName).find()&&
			   emailPattern.matcher(email).find()&&
			   yesNoPattern.matcher(isMathc3).find();
	}
	
	public static boolean validatePlayerId(String playerId) {
		return playerIdPattern.matcher(playerId).find();
	}
	
	public static String repeat(String string, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++) {
			result.append(string);
		}
		return result.toString();
	}
	
	public static String repeat(String string, String delimiter, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++) {
			result.append(string);
			if(i<count-1) {
				result.append(delimiter);
			}
		}
		return result.toString();
	}
	
	public static String jsonObject(String name, String value) {
		return "{\""+name+"\":\""+value+"\"}";
	}
	
	public static String jsonObject(Map<String, Object> vars) {
		StringBuilder result = new StringBuilder("{");
		for(Map.Entry<String, Object> e : vars.entrySet()) {
			Object value = e.getValue();
			result.append("\"").append(e.getKey()).append("\"").append(":");

			if(value instanceof String) {
				result.append("\"").append(value).append("\",");
			} else {
				result.append(value).append(",");
			}
		}
		return result.substring(0, result.lastIndexOf(","))+"}";
	}
	
	public static boolean simpleValidation(String[] names, Map<String, String> parameters) {
		for(String name : names) {
			if(!parameters.containsKey(name) || "".equals(parameters.get(name))) {
				return false;
			}
		}
		return true;
	}
	
	public static Map<String, String> parseUrlParameters(String url) {
		Map<String, String> reuslt = new HashMap<>();
		Map<String, List<String>> parameters = new QueryStringDecoder(url).parameters();
		for(Map.Entry<String, List<String>> p : parameters.entrySet()) {
			reuslt.put(p.getKey(), p.getValue().get(0));
		}
		return reuslt;
	}
	
	public static Map<String, String> parseParameters(String input) {
		Map<String, String> reuslt = new HashMap<>();
		if(input == null) {
			return reuslt;
		}
		
		String[] pairs = input.split("&");
		
		if(pairs.length < 2 && !input.contains("=")) {
			return reuslt;
		}
		
		for(String pair : pairs) {
			String[] params = pair.split("=");
			
			if(params.length < 2) {
				reuslt.put(params[0], "");
				continue;
			}
			reuslt.put(params[0].trim(), params[1].trim());
		}
		
		return reuslt;
	}
	
	public static String generateBigId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
