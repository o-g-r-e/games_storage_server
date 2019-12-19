package com.my.gamesdataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.AllowedUnconditionalRequest;

public class GameTemplate {
	private String type;
	private List<TableTemplate> tableTemplates;
	private List<AllowedUnconditionalRequest> allowedReuests;
	
	public GameTemplate(String type, List<TableTemplate> tableTemplates, List<AllowedUnconditionalRequest> allowedReuests) {
		this.type = type;
		this.tableTemplates = tableTemplates;
	}

	public List<TableTemplate> getTableTemplates() {
		return tableTemplates;
	}
	
	public String[] getTableNames() {
		List<String> result = new ArrayList<>();
		
		for(TableTemplate tableTemplate : tableTemplates) {
			result.add(tableTemplate.getName());
		}
		
		return result.toArray(new String[] {});
	}

	public String getType() {
		return type;
	}
}
