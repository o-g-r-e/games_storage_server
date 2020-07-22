package com.my.gamesdataserver.basedbclasses.queryclasses;

public class SimpleSqlExpression implements SqlExpression {
	private String name;
	private int type;
	private Object value;
	
	public SimpleSqlExpression(String name, int type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return name+"=?";
	}
}
