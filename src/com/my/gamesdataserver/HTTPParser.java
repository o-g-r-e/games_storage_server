package com.my.gamesdataserver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPParser {
	public String parseUrl(String req) {
		String result = null;
		Pattern urlPattern = Pattern.compile("GET ([\\w\\?=&/%{}\\.-]+) HTTP");
		Matcher matcher = urlPattern.matcher(req);
		
		if(matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
}
