package com.my.gamesdataserver;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
	private Command command;
	private Map<String, String> parameters;
	
	public Request(String url) {
		parameters = parseUrlParameters(url);
		command = parseCommand(url);
	}
	
	private boolean validateParameters(String[] names, Map<String, String> parameters) {
		if(parameters.size() != names.length) {
			return false;
		}
		
		for(String paramName : names) {
			if(!parameters.containsKey(paramName)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean validate() {
		switch (command) {
		case READ_SAVE:
			return validateParameters(new String[] {"key", "player_id"}, parameters);
		case REGISTER_PLAYER:
			return validateParameters(new String[] {"name", "player_id"}, parameters);
		case ADD_GAME:
			return validateParameters(new String[] {"name", "owner_name"}, parameters);
		case UPDATE_LEVEL:
			return validateParameters(new String[] {"key", "player_id", "level", "stars"}, parameters);
		case INSERT_LEVEL:
			return validateParameters(new String[] {"key", "player_id", "level", "stars"}, parameters);
		case REGISTER_OWNER:
			return validateParameters(new String[] {"name"}, parameters);
		case UPDATE_BOOST:
			return validateParameters(new String[] {"key", "player_id", "boost_data"}, parameters);
		case SHOW_MON:
			if(parameters.containsKey("key") && parameters.get("key").equals("vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4")) {
				return true;
			}
		default:
			break;
		}
		
		return false;
	}
	
	Command parseCommand(String url) {
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
					result.put(s[0], s[1]);
				}
			}
		}
		
		return result;
	}
	
	private Command recognizeCommandType(String command) {
		Command result = null;
		switch (command) {
		case "readSave":
			result = Command.READ_SAVE;
		case "regPlayer":
			result = Command.REGISTER_PLAYER;
		case "addGame":
			result = Command.ADD_GAME;
		case "updateLevel":
			result = Command.UPDATE_LEVEL;
		case "insertLevel":
			result = Command.INSERT_LEVEL;
		case "regOwner":
			result = Command.REGISTER_OWNER;
		case "updateBoost":
			result = Command.UPDATE_BOOST;
		case "show_mon":
			result = Command.SHOW_MON;
		}
		return result;
	}

	public Command getCommand() {
		return command;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}
}
