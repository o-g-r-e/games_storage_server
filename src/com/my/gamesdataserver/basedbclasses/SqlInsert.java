package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

public class SqlInsert extends SqlRequest {

	private List<List<CellData>> rowsToInsert = new ArrayList<>();
	
	/*public SqlInsert(String tableName, List<List<CellData>> rowsToInsert) throws JSONException {
		super(tableName, new ArrayList<>());
		this.rowsToInsert = rowsToInsert;
	}*/
	
	public SqlInsert(String tableName, List<CellData> rowToInsert) throws JSONException {
		super(tableName, new ArrayList<>());
		this.rowsToInsert.add(rowToInsert);
	}

	public List<List<CellData>> getRowToInsert() {
		return rowsToInsert;
	}

	public List<CellData> getRowToInsert(int index) {
		return rowsToInsert.get(index);
	}
	
	public void addInsertedValue(CellData val) {
		for(List<CellData> row : rowsToInsert) {
			row.add(val);
		}
	}
}
