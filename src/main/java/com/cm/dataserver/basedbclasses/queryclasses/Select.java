package com.cm.dataserver.basedbclasses.queryclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.basedbclasses.QueryTypedValue;

public class Select {
	private String table;
	private String fields;
	private SqlExpression where;
	private int limit;
	
	public Select(String table, String felds, SqlExpression where) {
		this.table = table;
		this.fields = felds;
		this.where = where;
		this.limit = 0;
	}
	
	public Select(String table, String felds) {
		this.table = table;
		this.fields = felds;
		this.where = null;
		this.limit = 0;
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

	public Select setWhere(SqlExpression where) {
		this.where = where;
		return this;
	}

	public Select setWhere(JSONArray jsonWhere) throws JSONException {
		this.where = Helpers.jsonWhereToObject(jsonWhere);
		return this;
	}

	public void setTable(String table) {
		this.table = table;
	}
}
