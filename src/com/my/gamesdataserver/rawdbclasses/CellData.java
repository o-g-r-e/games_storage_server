package com.my.gamesdataserver.rawdbclasses;

public class CellData {
	private int type;
	private String name;
	private Object value;
	
	public CellData(int type, String name, Object value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	
	public CellData(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}
}
