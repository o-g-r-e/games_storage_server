package com.cm.dataserver.basedbclasses;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;

public class TableTemplate {
	private String name;
	private Field[] cols;
	private List<TableIndex> indices = new ArrayList<>();
	private List<List<QueryTypedValue>> insert = new ArrayList<>();
	private String primaryKey = null;
	private List<ForeignKey> foreignKeys = new ArrayList<>();
	
	public TableTemplate(String name, Field[] cols, String primaryKey) {
		this.name = name;
		this.cols = cols;
		this.primaryKey = primaryKey;
	}
	
	public TableTemplate(String name, Field[] cols) {
		this.name = name;
		this.cols = cols;
	}
	
	public String getName() {
		return name;
	}
	public Field[] getCols() {
		return cols;
	}

	public List<TableIndex> getIndices() {
		return indices;
	}
	
	public void addIndex(TableIndex tableIndex) {
		indices.add(tableIndex);
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public List<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	public TableTemplate addForeignKey(ForeignKey foreignKey) {
		this.foreignKeys.add(foreignKey);
		return this;
	}

	public void addData(List<QueryTypedValue> values) {
		insert.add(values);
	}

	public List<List<QueryTypedValue>> getDataForInsert() {
		return insert;
	}

	public String buildSqlInsert(String namePrefix) {

		if(cols == null || cols.length <= 0 || insert == null || insert.size() <= 0) return null;

		List<String> fieldsNames = new ArrayList<>();
		List<String> queryValues = new ArrayList<>();

		for (Field field : cols) {
			if(field.isAutoIncrement()) continue;
			fieldsNames.add(field.getName());
			queryValues.add("?");
		}

		return String.format("INSERT INTO %s (%s) VALUES (%s)", namePrefix+name, 
																String.join(",", fieldsNames), 
																String.join(",", queryValues));
	}
}