package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPRequestFilter {
	private List<String> forbiddenRequestsList;
	
	HTTPRequestFilter() {
		forbiddenRequestsList = new ArrayList<String>();
		initLists(forbiddenRequestsList);
	}
	
	public boolean filterForbiddens(String urlPath) {
		for(String s : forbiddenRequestsList) {
			if(urlPath.equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	private void initLists(List<String> forbiddenRequestsList) {
		if(forbiddenRequestsList != null) {
			forbiddenRequestsList.add("/favicon.ico");
		}
	}
}
