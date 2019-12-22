package com.my.gamesdataserver.basedbclasses;

import java.util.List;

import org.json.JSONException;

import com.my.gamesdataserver.SqlExpression;

public class SqlSelect extends SqlRequest {
	String[] fields = null;
	
	public SqlSelect(String tableName, List<SqlExpression> whereExpression) throws JSONException {
		super(tableName, whereExpression);
	}
	
	public SqlSelect(String tableName) throws JSONException {
		super(tableName);
	}

	public String[] getFields() {
		return fields;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}
}
