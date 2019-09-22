package com.my.gamesdataserver;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequest {
	private static final Pattern HTTP_REQUEST_PATTERN = Pattern.compile("^([A-Z]+)\\s(.+\\?(.+))\\sHTTP[\\s\\S]+(\\n\\n|\\r\\n\\r\\n)([\\s\\S]*)");
	private String type;
	private String url;
	private Map<String, String> urlParameters = new HashMap<String, String>();
	private Map<String, String> headers = new HashMap<String, String>();
	private String content;
	
	public HttpRequest(String httpRequest) {
		Matcher matcher = HTTP_REQUEST_PATTERN.matcher(httpRequest);
		
		if(matcher.find()) {
			type = matcher.group(1);
			url = matcher.group(2);
			String[] parameters = matcher.group(3).split("&");
			for (int i = 0; i < parameters.length; i++) {
				urlParameters.put(parameters[i].substring(0, parameters[i].indexOf("=")),parameters[i].substring(parameters[i].indexOf("=")+1));
			}
			content = matcher.group(5);
		}
	}
	
	public String getUrl() {
		return url;
	}
	
	public Map<String, String> getUrlParametrs() {
		return urlParameters;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getType() {
		return type;
	}
}
