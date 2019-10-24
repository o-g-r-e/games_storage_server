package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SqlRequest {
	protected String tableName;
	protected List<CellData> whereExpression = new ArrayList<>();
	
	public SqlRequest(String tableName) throws JSONException {
		this.tableName = tableName;
	}
	
	public SqlRequest(String tableName, List<CellData> whereExpression) throws JSONException {
		this.whereExpression = whereExpression;
		this.tableName = tableName;
	}
	
	public SqlRequest(String tableName, String json) throws JSONException {
		parseWhereExpression(json);
		this.tableName = tableName;
	}
	
	private void parseWhereExpression(String jsonCallDataArray) throws JSONException {
		whereExpression = parseCellDataRow(jsonCallDataArray);
	}
	
	protected List<CellData> parseCellDataRow(String jsonCallDataArray) throws JSONException {
		List<CellData> result = new ArrayList<>();
		JSONArray jsonArray = new JSONArray(jsonCallDataArray);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject cellObject = jsonArray.getJSONObject(i);
			if(cellObject.has("type")) {
				result.add(new CellData(DataBaseInterface.parseDataType(cellObject.getString("type")), cellObject.getString("name"), cellObject.getString("value")));
			} else {
				result.add(new CellData(cellObject.getString("name"), cellObject.getString("value")));
			}
		}
		return result;
	}

	public String getTableName() {
		return tableName;
	}

	public List<CellData> getWhereExpression() {
		return whereExpression;
	}
}
