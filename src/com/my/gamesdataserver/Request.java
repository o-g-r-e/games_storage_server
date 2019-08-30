package com.my.gamesdataserver;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
	private Type commandType;
	private Map<String, String> parameters;
	private String[] validationSchema;
	
	public enum Type {READ_SAVE, 
					  REGISTER_PLAYER, 
					  ADD_GAME, 
					  UPDATE_SAVE,
					  REGISTER_OWNER, 
					  UPDATE_BOOST,
					  MONITOR_DATA}
	
	public Request(String urlRequest) {
		parameters = parseUrlParameters(urlRequest);
		parseCommand(urlRequest);
	}
	
	public boolean validateParametersWithSchema() {
		if(parameters.size() != validationSchema.length) {
			return false;
		}
		
		for(String paramName : validationSchema) {
			if(!parameters.containsKey(paramName)) {
				return false;
			}
		}
		
		return true;
	}
	
	private void parseCommand(String urlRequest) {
		Pattern p = Pattern.compile("\\/(\\w+)(\\?|$)");
		Matcher m = p.matcher(urlRequest);
		
		if(m.find()) {
			String com = m.group(1);
			switch (com) {
			case "readSave":
				commandType = Type.READ_SAVE;
				validationSchema = new String[] {"game_api_key", "player_id"};
				break;
			case "regPlayer":
				commandType = Type.REGISTER_PLAYER;
				validationSchema = new String[] {"player_name", "player_id", "game_api_key"};
				break;
			case "addGame":
				commandType = Type.ADD_GAME;
				validationSchema = new String[] {"name", "owner_name"};
				break;
			case "updateSave":
				commandType = Type.UPDATE_SAVE;
				validationSchema = new String[] {"game_api_key", "player_id", "save_data"};
				break;
			case "regOwner":
				commandType = Type.REGISTER_OWNER;
				validationSchema = new String[] {"name"};
				break;
			case "updateBoost":
				commandType = Type.UPDATE_BOOST;
				validationSchema = new String[] {"game_api_key", "player_id", "boost_data"};
				break;
			case "monitor_data":
				commandType = Type.MONITOR_DATA;
				validationSchema = new String[] {"key"};
				break;
			default: 
				break;
			}
		}
	}
	
	Map<String, String> parseUrlParameters(String url) {
		Map<String, String> result = new HashMap<String, String>();
		String[] pairs = url.substring(url.indexOf("?")+1).split("&");
		
		if(pairs != null && pairs.length > 0) {
			for(String p : pairs) {
				String[] s = p.split("=");
				if(s != null && s.length >= 2) {
					result.put(s[0], urlDecode(s[1]));
				}
			}
		}
		
		return result;
	}
	
	private String urlDecode(String parameterValue) {
		parameterValue = parameterValue.replaceAll("%20", " ");
		parameterValue = parameterValue.replaceAll("%22", "\"");
		return parameterValue;
	}

	public Type getCommand() {
		return commandType;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}
}
