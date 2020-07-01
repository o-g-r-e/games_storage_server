package com.my.gamesdataserver.basedbclasses;

public class TypedValue {
	int type;
	Object value;
	
	public TypedValue(int type, Object value) {
		this.type = type;
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}