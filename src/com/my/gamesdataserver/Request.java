package com.my.gamesdataserver;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
	private Commands command;
	private Map<String, String> parameters;
	
	public Request(String url) {
		parameters = parseUrlParameters(url);
		command = parseCommand(url);
	}
	
	private boolean validateParameters(String[] namesMustFound, Map<String, String> parameters) {
		if(parameters.size() != namesMustFound.length) {
			return false;
		}
		
		for(String paramName : namesMustFound) {
			if(!parameters.containsKey(paramName)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean commandValidate() {
		
		if (command == null) {
			return false;
		}
		
		String[] pNames = null;
		
		switch (command) {
		case READ_SAVE:
			pNames = new String[] {"game_api_key", "player_id"};
			break;
		case REGISTER_PLAYER:
			pNames = new String[] {"player_name", "player_id", "game_api_key"};
			break;
		case ADD_GAME:
			pNames = new String[] {"name", "owner_name"};
			break;
		case UPDATE_SAVE :
			pNames = new String[] {"game_api_key", "player_id", "save_data"};
			break;
		case REGISTER_OWNER:
			pNames = new String[] {"name"};
			break;
		case UPDATE_BOOST:
			pNames = new String[] {"game_api_key", "player_id", "boost_data"};
			break;
		case MONITOR_DATA:
			pNames = new String[] {"key"};
			break;
		}
		
		if(pNames == null) {
			return false;
		}
		
		return validateParameters(pNames, parameters);
	}
	
	private Commands parseCommand(String url) {
		Pattern p = Pattern.compile("\\/(\\w+)(\\?|$)");
		Matcher m = p.matcher(url);
		
		if(m.find()) {
			String command = m.group(1);
			return recognizeCommandType(command);
		}
		return null;
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
	
	private Commands recognizeCommandType(String command) {
		switch (command) {
		case "readSave":
			return Commands.READ_SAVE;
		case "regPlayer":
			return Commands.REGISTER_PLAYER;
		case "addGame":
			return Commands.ADD_GAME;
		case "updateSave":
			return Commands.UPDATE_SAVE;
		case "regOwner":
			return Commands.REGISTER_OWNER;
		case "updateBoost":
			return Commands.UPDATE_BOOST;
		case "monitor_data":
			return Commands.MONITOR_DATA;
		}
		
		return null;
	}

	public Commands getCommand() {
		return command;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}
}
