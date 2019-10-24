package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class SqlUpdate extends SqlRequest {
	private List<CellData> updatesData = new ArrayList<>();
	
	public SqlUpdate(String tableName, String jsonWhereData, String jsonSetData) throws JSONException {
		super(tableName, jsonWhereData);
		updatesData = parseCellDataRow(jsonSetData);
	}
	
	public SqlUpdate(String tableName, List<CellData> whereData, List<CellData> updatesData) throws JSONException {
		super(tableName, whereData);
		this.updatesData = updatesData;
	}

	public List<CellData> getUpdatesData() {
		return updatesData;
	}
}
