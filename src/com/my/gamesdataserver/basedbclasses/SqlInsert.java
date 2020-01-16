package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.my.gamesdataserver.SqlExpression;

public class SqlInsert extends SqlRequest {

	private List<List<SqlExpression>> rowsToInsert = new ArrayList<>();
	
	/*public SqlInsert(String tableName, List<List<CellData>> rowsToInsert) throws JSONException {
		super(tableName, new ArrayList<>());
		this.rowsToInsert = rowsToInsert;
	}*/
	
	public SqlInsert(String tableName, List<SqlExpression> rowToInsert) throws JSONException {
		super(tableName, new ArrayList<>());
		this.rowsToInsert.add(rowToInsert);
	}

	public List<List<SqlExpression>> getRowToInsert() {
		return rowsToInsert;
	}

	public List<SqlExpression> getRowToInsert(int index) {
		return rowsToInsert.get(index);
	}
	
	public void addInsertedValue(SqlExpression val) {
		for(List<SqlExpression> row : rowsToInsert) {
			row.add(val);
		}
	}
}
