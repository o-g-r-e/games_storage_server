package com.my.gamesdataserver;

import java.util.ArrayList;
import java.util.List;

public class GameTemplate {
	public enum Types {MATCH3};
	private Types type;
	private List<TableTemplate> tables;
	
	public GameTemplate(Types type, List<TableTemplate> tables) {
		this.type = type;
		this.tables = tables;
	}

	public Types getType() {
		return type;
	}

	public List<TableTemplate> getTables() {
		return tables;
	}
	
	public String[] getTableNames() {
		List<String> result = new ArrayList<>();
		
		for(TableTemplate tt : tables) {
			result.add(tt.getName());
		}
		
		return result.toArray(new String[] {});
	}
}
