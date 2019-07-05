package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class Access {
	private List<String> allowedPathes;
	public final static String contentAccessKey = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	public final static String boostPurchaseToken = "vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4";
	
	public Access() {
		allowedPathes = new ArrayList<String>();
		allowedPathes.add("/monitor");
	}
	
	public boolean isAllowedPath(String urlPath) {
		for (String allowedPath : allowedPathes) {
			if(urlPath.startsWith(allowedPath)) {
				return true;
			}
		}
		return false;
	}
}
