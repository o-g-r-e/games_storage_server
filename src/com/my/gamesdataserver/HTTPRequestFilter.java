package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class HTTPRequestFilter {
	private List<String> forbiddenRequestsList;
	private List<String> allowedRequestsList;
	
	HTTPRequestFilter() {
		forbiddenRequestsList = new ArrayList<String>();
		allowedRequestsList = new ArrayList<String>();
		initLists(forbiddenRequestsList, allowedRequestsList);
	}
	
	public boolean filterForbiddens(String urlPath) {
		for(String s : forbiddenRequestsList) {
			if(urlPath.equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean filterAllowed(String urlPath) {
		for(String s : allowedRequestsList) {
			if(urlPath.equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	private void initLists(List<String> forbiddenRequestsList, List<String> allowedRequestsList) {
		if(forbiddenRequestsList != null) {
			forbiddenRequestsList.add("/favicon.ico");
		}
		
		if(allowedRequestsList != null) {
			allowedRequestsList.add("/monitor");
		}
	}
}
