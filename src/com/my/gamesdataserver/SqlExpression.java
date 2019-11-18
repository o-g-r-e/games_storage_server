package com.my.gamesdataserver;

public class SqlExpression {
	private int type;
	private String name;
	private Object value;
	
	public SqlExpression(int type, String name, Object value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	
	public SqlExpression(String name, Object value) {
		this.type = -1;
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

	public void setType(int type) {
		this.type = type;
	}
}
