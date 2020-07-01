package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;

public class SqlUpdate {

	private String tableName;
	private Row set;
	private SqlExpression where;
	
	public String getTableName() {
		return tableName;
	}
	public Row getSet() {
		return set;
	}
	public SqlExpression getWhere() {
		return where;
	}
	public SqlUpdate(String tableName, Row set, SqlExpression where) {
		this.tableName = tableName;
		this.set = set;
		this.where = where;
	}
	
	public SqlUpdate(JSONObject jsonQuery) throws JSONException {
		set = new Row();
		tableName = jsonQuery.getString("table_name");
		
		JSONArray jsonQuerySet = jsonQuery.getJSONArray("set");
		
		for (int i = 0; i < jsonQuerySet.length(); i++) {
			JSONObject jsonCell = jsonQuerySet.getJSONObject(i);
			int type = jsonCell.isNull("type") ? Types.NULL : DataBaseMethods.serverTypeToMysqType(jsonCell.getString("type"));
			set.addCell(new CellData(type, jsonCell.getString("name"), jsonCell.getString("value")));
		}
		
		if(!jsonQuery.isNull("condition")) {
			where = new SqlExpression(jsonQuery.getJSONArray("condition"));
		}
	}
	
	public void addCondition(SqlWhereValue e) {
		where.addValue(e);
	}
	
	/*private List<SqlExpression> updatesData = new ArrayList<>();
	
	public SqlUpdate(String tableName, List<SqlExpression> whereData, List<SqlExpression> updatesData) throws JSONException {
		super(tableName, whereData);
		this.updatesData = updatesData;
	}

	public List<SqlExpression> getUpdatesData() {
		return updatesData;
	}*/
}
