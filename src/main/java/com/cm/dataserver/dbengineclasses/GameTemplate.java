package com.cm.dataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.List;

import com.cm.dataserver.basedbclasses.TableTemplate;


public class GameTemplate {
	private String name;
	private List<TableTemplate> tableTemplates;
	
	public GameTemplate(String name, List<TableTemplate> tableTemplates) {
		this.name = name;
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
		return name;
	}
}
