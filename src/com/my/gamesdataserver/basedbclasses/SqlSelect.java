package com.my.gamesdataserver.basedbclasses;

import java.util.List;

import org.json.JSONException;

public class SqlSelect extends SqlRequest {

	public SqlSelect(String tableName, List<CellData> whereExpression) throws JSONException {
		super(tableName, whereExpression);
	}
	
	public SqlSelect(String tableName, String json) throws JSONException {
		super(tableName, json);
	}
	
}
