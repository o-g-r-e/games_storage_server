package com.my.gamesdataserver.basedbclasses;

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
		this.type = -1;
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

	public void setType(int type) {
		this.type = type;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
