package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.SqlExpression;

public class SqlRequest {
	protected String tableName;
	protected List<SqlExpression> whereExpression = new ArrayList<>();
	
	public SqlRequest(String tableName) throws JSONException {
		this.tableName = tableName;
	}
	
	public SqlRequest(String tableName, List<SqlExpression> whereExpression) throws JSONException {
		this.whereExpression = whereExpression;
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public List<SqlExpression> getWhereExpression() {
		return whereExpression;
	}
}
