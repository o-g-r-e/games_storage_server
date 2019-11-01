package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row {
	private List<CellData> cells = new ArrayList<>();
	private Map<String, CellData> mapCells = new HashMap<>();
	
	public Row(List<CellData> cells) {
		this.cells = cells;
		for(CellData cell : cells) {
			mapCells.put(cell.getName(), cell);
		}
	}

	public CellData getCell(String fieldName) {
		return mapCells.get(fieldName);
	}

	public boolean containsCell(String fieldName) {
		return mapCells.containsKey(fieldName);
	}

	public List<CellData> getCells() {
		return cells;
	}
}
