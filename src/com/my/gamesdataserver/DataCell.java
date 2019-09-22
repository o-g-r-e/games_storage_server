package com.my.gamesdataserver;

public class DataCell {
	private int type;
	private String name;
	private Object value;
	
	public DataCell(int type, String name, Object value) {
		this.type = type;
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
