package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class Access {
	private List<String> permittedPathes;
	private List<String> requestsForDiscard;
	public final static String contentAccessKey = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	public final static String boostPurchaseToken = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	
	public Access() {
		permittedPathes = new ArrayList<String>();
		permittedPathes.add("/html");
		
		requestsForDiscard = new ArrayList<String>();
		//requestsForDiscard.add("/favicon.ico");
	}
	
	public boolean isWrongSymbols(String urlPath) {
		return urlPath.contains("./") || urlPath.contains(".\\") || urlPath.contains("..");
	}
	
	public boolean isDiscardRequest(String urlPath) {
		for(String s : requestsForDiscard) {
			if(urlPath.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
