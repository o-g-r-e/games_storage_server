package com.cm.dataserver;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerURI {
	public enum RequestGroup {BASE, API, PLAYER_REQUEST, ALLOWED_REQUEST, BAD, GAME, MESSAGE};
	
	public enum RequestName {
		GEN_API_KEY, 
		REGISTER_GAME, 
		CREATE_SPEC_REQUEST, 
		SPEC_REQUEST_LIST, 
		DELETE_GAME, 
		PLAYER_AUTHORIZATION, 
		SELECT, 
		INSERT, 
		UPDATE, 
		LEVEL, 
		BOOST,
		LEADBOARD,
		PLAYERPROGRESS,
		MAXPLAYERPROGRESS,
		SEND_MESSAGE,
		FETCH_ALL_MESSAGES,
		DELETE_MESSAGE,
		CREATE_LIFE_REQUEST,
		CONFIRM_LIFE_REQUEST,
		DENY_LIFE_REQUEST,
		LIFE_REQUESTS,
		ACCEPT_LIFE,
		REFUSE_LIFE,
		SEND_LIFE};
		
		private static Map<String, ServerURI.RequestGroup> requestGroupMap;
		
		private static Map<String, ServerURI.RequestName> requestMap;
		
		static {
			
			requestGroupMap = new HashMap<>();
			requestGroupMap.put("api", RequestGroup.API);
			requestGroupMap.put("player", RequestGroup.PLAYER_REQUEST);
			requestGroupMap.put("system", RequestGroup.BASE);
			requestGroupMap.put("special", RequestGroup.ALLOWED_REQUEST);
			requestGroupMap.put("game", RequestGroup.GAME);
			requestGroupMap.put("message", RequestGroup.MESSAGE);
			
			requestMap = new HashMap<>();
			requestMap.put("/system/register_game", RequestName.REGISTER_GAME);
			requestMap.put("/system/set_special_request", RequestName.CREATE_SPEC_REQUEST);
			requestMap.put("/system/special_request_list", RequestName.SPEC_REQUEST_LIST);
			requestMap.put("/system/delete_game", RequestName.DELETE_GAME);
			requestMap.put("/player/authorization", RequestName.PLAYER_AUTHORIZATION);
			requestMap.put("/api/select", RequestName.SELECT);
			requestMap.put("/api/insert", RequestName.INSERT);
			requestMap.put("/api/update", RequestName.UPDATE);
			requestMap.put("/game/levels", RequestName.LEVEL);
			requestMap.put("/game/boosts", RequestName.BOOST);
			requestMap.put("/game/leaderboard", RequestName.LEADBOARD);
			requestMap.put("/game/playerprogress", RequestName.PLAYERPROGRESS);
			requestMap.put("/game/maxplayerprogress", RequestName.MAXPLAYERPROGRESS);
			requestMap.put("/game/create_life_request", RequestName.CREATE_LIFE_REQUEST);
			requestMap.put("/game/confirm_life_request", RequestName.CONFIRM_LIFE_REQUEST);
			requestMap.put("/game/deny_life_request", RequestName.DENY_LIFE_REQUEST);
			requestMap.put("/game/life_requests", RequestName.LIFE_REQUESTS);
			requestMap.put("/game/accept_life", RequestName.ACCEPT_LIFE);
			requestMap.put("/game/refuse_life", RequestName.REFUSE_LIFE);
			requestMap.put("/game/send_life", RequestName.SEND_LIFE);
			requestMap.put("/message/send", RequestName.SEND_MESSAGE);
			requestMap.put("/message/fetch_my_messages", RequestName.FETCH_ALL_MESSAGES);
			requestMap.put("/message/delete", RequestName.DELETE_MESSAGE);
		}
		
		private static Pattern requestUriPattern = Pattern.compile("^\\/\\w+\\/\\w+");
		private static Pattern requestGroupPattern = Pattern.compile("^\\/(\\w+)\\/?");
		
		public static RequestName parseRequestUri(String urlPath) {
			Matcher requestNameMatcher = requestUriPattern.matcher(urlPath);
			if(requestNameMatcher.find()) return requestMap.get(requestNameMatcher.group());
			return null;
		}
		
		public static RequestGroup requestGroup(String uri) {
			Matcher requestGroupMatcher = requestGroupPattern.matcher(uri);
			if(requestGroupMatcher.find()) return requestGroupMap.get(requestGroupMatcher.group(1));
			return RequestGroup.BAD;
		}
		
		public static boolean isGameDependsRequest(RequestGroup requestGroup) {
			return requestGroup == RequestGroup.PLAYER_REQUEST || requestGroup == RequestGroup.API            || 
				   requestGroup == RequestGroup.GAME           || requestGroup == RequestGroup.ALLOWED_REQUEST;
		}
}
