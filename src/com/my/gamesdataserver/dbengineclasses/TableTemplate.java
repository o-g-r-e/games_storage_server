package com.my.gamesdataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.basedbclasses.Field;

public class TableTemplate {
	private String name;
	private Field[] cols;
	private List<TableIndex> indices = new ArrayList<>();
	private String primaryKey = null;
	
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
}
