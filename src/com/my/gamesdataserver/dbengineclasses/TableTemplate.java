package com.my.gamesdataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.basedbclasses.ColData;

public class TableTemplate {
	private String name;
	private ColData[] cols;
	private List<TableIndex> indices = new ArrayList<>();
	private String primaryKey = null;
	
	public TableTemplate(String name, ColData[] cols, String primaryKey) {
		this.name = name;
		this.cols = cols;
		this.primaryKey = primaryKey;
	}
	
	public TableTemplate(String name, ColData[] cols) {
		this.name = name;
		this.cols = cols;
	}
	
	public String getName() {
		return name;
	}
	public ColData[] getCols() {
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
