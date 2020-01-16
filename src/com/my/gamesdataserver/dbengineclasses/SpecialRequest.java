package com.my.gamesdataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpecialRequest {
	private int gameId;
	private String requestName;
	private String table;
	private String fields;
	private List<String> fieldsList;
	
	public SpecialRequest(int gameId, String requestName, String table, String fields) {
		this.gameId = gameId;
		this.requestName = requestName;
		this.table = table;
		this.fields = fields;
		fieldsList = new ArrayList<>();
		String[] fieldsArray = fields.split(",");
		for(String fieldValue : fieldsArray) {
			fieldsList.add(fieldValue.trim());
		}
	}

	public int getGameId() {
		return gameId;
	}

	public String getRequestName() {
		return requestName;
	}

	public String getTable() {
		return table;
	}

	public String getFields() {
		return fields;
	}

	public List<String> getFieldsList() {
		return fieldsList;
	}
}
