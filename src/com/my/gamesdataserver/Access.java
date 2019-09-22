package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class Access {
	private List<String> allowedPathes;
	private List<String> forbiddenRequestsList;
	public final static String contentAccessKey = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	public final static String boostPurchaseToken = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	
	public Access() {
		allowedPathes = new ArrayList<String>();
		allowedPathes.add("/html");
		
		forbiddenRequestsList = new ArrayList<String>();
		forbiddenRequestsList.add("/favicon.ico");
	}
	
	public boolean isAllowedPath(String urlPath) {
		for (String allowedPath : allowedPathes) {
			if(urlPath.startsWith(allowedPath)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isForbidden(String urlPath) {
		for(String s : forbiddenRequestsList) {
			if(urlPath.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
