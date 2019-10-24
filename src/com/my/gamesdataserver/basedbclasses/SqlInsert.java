package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

public class SqlInsert extends SqlRequest {

	private List<CellData> rowToInsert = new ArrayList<>();
	
	public SqlInsert(String tableName, String jsonWhereData, String jsonInsertData) throws JSONException {
		super(tableName, jsonWhereData);
		rowToInsert = parseCellDataRow(jsonInsertData);
	}
	
	public SqlInsert(String tableName, List<CellData> whereDataRows, List<CellData> rowToInsert) throws JSONException {
		super(tableName, whereDataRows);
		this.rowToInsert = rowToInsert;
	}

	public List<CellData> getRowToInsert() {
		return rowToInsert;
	}
}
