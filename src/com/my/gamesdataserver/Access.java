package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class Access {
	private List<String> allowedPathes;
	
	public Access() {
		allowedPathes = new ArrayList<String>();
		allowedPathes.add("/monitor");
	}
	
	public boolean isAllowedPath(String urlPath) {
		for (String allowedPath : allowedPathes) {
			if(urlPath.equals(allowedPath)) {
				return true;
			}
		}
		return false;
	}
}
