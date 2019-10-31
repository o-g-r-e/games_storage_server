package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

public class TableTemplate {
	private String name;
	private ColData[] cols;
	private List<TableIndex> indices = new ArrayList<>();
	
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
}
