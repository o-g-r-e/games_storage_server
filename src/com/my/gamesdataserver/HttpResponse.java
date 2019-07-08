package com.my.gamesdataserver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpResponse {
	private String version;
	private int statusCode;
	
	private String content;
	
	public HttpResponse(String version, int statusCode, String content) {
		this.version = version;
		this.statusCode = statusCode;
		this.content = content;
	}
	
	public String getStatusTextValue() {
		switch (statusCode) {
		case 200:
			return "OK";
		}
		return "";
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getContent() {
		return content;
	}
	
	public String getCutContent(int maxLines) {
		StringBuilder result = new StringBuilder();
		Matcher m = Pattern.compile(".*\r\n|.*\r|.*\n").matcher(content);
		int foundLines = 0;
		while(m.find()) {
			foundLines++;
			result.append(m.group());
			if(foundLines >= maxLines) {
				break;
			}
		}
		
		if(foundLines < maxLines) {
			return content;
		}
		
		return result.toString();
	}
	
	public String getContentCharacters(int countOfCharacters) {
		if(countOfCharacters > content.length()) {
			return content;
		}
		return content.substring(0, countOfCharacters);
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String getHeader() {
		String result = "HTTP/"+version+" "+statusCode+" "+getStatusTextValue();
		if(content != null && content.length() > 0) {
			result += "\r\nContent-Length: "+content.length()+"\r\n\r\n";
		}
		return result;
	}
	
	@Override
	public String toString() {
		String result = "HTTP/"+version+" "+statusCode+" "+getStatusTextValue();
		if(content != null && content.length() > 0) {
			result += "\r\nContent-Length: "+content.length()+"\r\n\r\n"+content;
		}
		return result;
	}
}
