package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.my.gamesdataserver.SqlExpression;

public class SqlSelect extends SqlRequest {
	List<String> fields;
	
	public SqlSelect(String tableName, List<SqlExpression> whereExpression) throws JSONException {
		super(tableName, whereExpression);
		fields = new ArrayList<>();
	}
	
	public SqlSelect(String tableName) throws JSONException {
		super(tableName);
		fields = new ArrayList<>();
	}

	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}
}
