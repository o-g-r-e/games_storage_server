package com.my.gamesdataserver.basedbclasses;

import java.util.List;

import org.json.JSONException;

import com.my.gamesdataserver.SqlExpression;

public class SqlSelect extends SqlRequest {

	public SqlSelect(String tableName, List<SqlExpression> whereExpression) throws JSONException {
		super(tableName, whereExpression);
	}
	
}
