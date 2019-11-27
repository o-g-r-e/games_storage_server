package com.my.gamesdataserver.basedbclasses;

import org.json.JSONException;

public class Increment extends SqlRequest {
	private String fieldName;
	
	public Increment(String tableName, String fieldName) throws JSONException {
		super(tableName);
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}
}
