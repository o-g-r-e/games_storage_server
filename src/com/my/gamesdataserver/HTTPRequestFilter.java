package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class HTTPRequestFilter {
	private List<String> deprecatedRequestsList;
	
	HTTPRequestFilter() {
		deprecatedRequestsList = new ArrayList<String>();
		initDeprecatedRequestsList(deprecatedRequestsList);
	}
	
	public boolean urlFilter(String url) {
		if("/favicon.ico".equals(url)) {
			return true;
		}
		return false;
	}
	
	private void initDeprecatedRequestsList(List<String> deprecatedRequestsList) {
		if(deprecatedRequestsList == null) {
			return;
		}
		deprecatedRequestsList.add("/favicon.ico");
	}
}
