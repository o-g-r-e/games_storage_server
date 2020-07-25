package com.my.gamesdataserver.basedbclasses.queryclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.QueryTypedValue;

public class Select {
	private String table;
	private String fields;
	private SqlExpression where;
	private int limit;
	
	public Select(String table, String felds, SqlExpression where, int limit) {
		this.table = table;
		this.fields = felds;
		this.where = where;
		this.limit = limit;
	}
	
	public Select(JSONObject jsonQuery) throws JSONException {
		table = jsonQuery.getString("table");
		fields = jsonQuery.has("fields")?jsonQuery.getJSONArray("fields").join(","):"*";
		
		if(jsonQuery.has("condition")) {
			where = Helpers.jsonWhereToObject(jsonQuery.getJSONArray("condition"));
		}
	}
	
	@Override
	public String toString() {
		return "SELECT "+fields+" FROM "+table+(where==null?"":" WHERE "+where.toString());
	}

	public String getTable() {
		return table;
	}

	public String getFields() {
		return fields;
	}

	public SqlExpression getWhere() {
		return where;
	}

	public int getLimit() {
		return limit;
	}

	public void setWhere(SqlExpression where) {
		this.where = where;
	}

	public void setTable(String table) {
		this.table = table;
	}
}
