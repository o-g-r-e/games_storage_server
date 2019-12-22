package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Row {
	private Map<String, CellData> mapCells = new TreeMap<>();
	
	public Row(List<CellData> cells) {
		for(CellData cell : cells) {
			mapCells.put(cell.getName(), cell);
		}
	}

	public CellData getCell(String fieldName) {
		return mapCells.get(fieldName);
	}
	
	public Object get(String fieldName) {
		return mapCells.get(fieldName).getValue();
	}

	public boolean containsCell(String fieldName) {
		return mapCells.containsKey(fieldName);
	}
	
	public Row removeCell(String fieldName) {
		mapCells.remove(fieldName);
		return this;
	}
	
	public List<CellData> toList() {
		List<CellData> result = new ArrayList<>();
		
		for(Map.Entry<String, CellData> cell : mapCells.entrySet()) {
			result.add(cell.getValue());
		}
		
		return result;
	}
	
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int j=0;
		for(Map.Entry<String, CellData> cell : mapCells.entrySet()) {
			CellData cellData = cell.getValue();
			sb.append("\"").append(cellData.getName()).append("\":");
			if(cellData.getType() == Types.VARCHAR) {
				sb.append("\"").append(cellData.getValue()).append("\"");
			} else {
				sb.append(cellData.getValue());
			}
			if(j++ < mapCells.size()-1) {
				sb.append(",");
			}
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static String rowsToJson(List<Row> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < rows.size(); i++) {
			sb.append(rows.get(i).toJson());
			if(i < rows.size()-1) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
}
