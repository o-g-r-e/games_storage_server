package com.my.gamesdataserver.deprecated;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpRequest {
	private static final Pattern HTTP_REQUEST_PATTERN = Pattern.compile("^([A-Z]+)\\s([\\w\\.\\/-]+)(\\?(.*))?\\sHTTP[\\s\\S]+(\\n\\n|\\r\\n\\r\\n)([\\s\\S]*)");
	private String type;
	private String url;
	private Map<String, String> urlParameters = new HashMap<String, String>();
	private Map<String, String> contentParameters = new HashMap<String, String>();
	private Map<String, String> headers = new HashMap<String, String>();
	private String content;
	
	public HttpRequest() {
		
	}
	
	public HttpRequest(String httpRequest) {
		parse(httpRequest);
	}
	
	public void parse(String httpRequest) {
		Matcher matcher = HTTP_REQUEST_PATTERN.matcher(httpRequest);
		
		if(matcher.find()) {
			type = matcher.group(1);
			url = matcher.group(2);
			String parametersInUrl = matcher.group(4)==null?"":matcher.group(4);
			urlParameters = parseParameters(parametersInUrl.replaceAll("\\+", " ").replaceAll("%40", "@"));
			content = matcher.group(6).replaceAll("\\+", " ").replaceAll("%40", "@");
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
	
	private Map<String, String> parseParameters(String input) {
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
			reuslt.put(params[0], params[1]);
		}
		
		return reuslt;
	}
	
	public Map<String, String> parseContentWithParameters() {
		if(contentParameters.size() <= 0) {
			contentParameters = parseParameters(getContent());
		}
		return contentParameters;
	}

	public void clear() {
		type = "";
		url = "";
		urlParameters.clear();
		contentParameters.clear();
		headers.clear();
		content = "";
	}
}
