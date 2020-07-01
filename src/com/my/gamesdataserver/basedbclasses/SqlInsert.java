package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;

public class SqlInsert {
	private String tableName;
	private List<Row> rows;
	
	public String getTableName() {
		return tableName;
	}
	
	public List<Row> getRows() {
		return rows;
	}
	
	public SqlInsert(String tableName, List<Row> rows) {
		this.tableName = tableName;
		this.rows = rows;
	}
	
	public SqlInsert(JSONObject jsonQuery) throws JSONException {
		
		class FieldData {
			public int type;
			public String name;
			
			public FieldData(int type, String name) {
				this.type = type;
				this.name = name;
			}
		}
		
		tableName = jsonQuery.getString("table");
		rows = new ArrayList<>();
		JSONArray jsonFields = jsonQuery.getJSONArray("fields");
		
		List<FieldData> fieldsData = new ArrayList<>();
		for (int i = 0; i < jsonFields.length(); i++) {
			JSONObject jsonField = jsonFields.getJSONObject(i);
			int type = jsonField.isNull("type") ? Types.NULL : DataBaseMethods.serverTypeToMysqType(jsonField.getString("type"));
			fieldsData.add(new FieldData(type, jsonField.getString("name")));
		}
		
		JSONArray jsonValues = jsonQuery.getJSONArray("values");
		
		for (int i = 0; i < jsonValues.length(); i++) {
			JSONArray jsonValuesRow = jsonValues.getJSONArray(i);
			Row resultRow = new Row();
			for (int j = 0; j < jsonValuesRow.length(); j++) {
				Object value = null;
				int fieldType = fieldsData.get(j).type;
				switch (fieldType) {
				case Types.INTEGER:
					value = jsonValuesRow.getInt(j);
					break;
				case Types.FLOAT:
					value = jsonValuesRow.getLong(j);
					break;
				case Types.VARCHAR:
					value = jsonValuesRow.getString(j);
					break;
				default:
					value = jsonValuesRow.getString(j);
					break;
				}
				resultRow.addCell(new CellData(fieldType, fieldsData.get(j).name, value));
			}
			rows.add(resultRow);
		}
	}
	
	public void addValue(CellData value) {
		for(Row r : rows) {
			r.addCell(value);
		}
	}
	
	//private List<List<SqlExpression>> rowsToInsert = new ArrayList<>();
	
	/*public SqlInsert(String tableName, List<List<CellData>> rowsToInsert) throws JSONException {
		super(tableName, new ArrayList<>());
		this.rowsToInsert = rowsToInsert;
	}*/
	
	/*public SqlInsert(String tableName, List<SqlExpression> rowToInsert) throws JSONException {
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
	}*/
}
