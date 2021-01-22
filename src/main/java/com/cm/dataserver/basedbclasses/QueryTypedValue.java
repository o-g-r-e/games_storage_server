package com.cm.dataserver.basedbclasses;

import java.sql.Types;

public class QueryTypedValue {
	private int type;
	private Object value;
	
	public QueryTypedValue(int type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public QueryTypedValue(Object value) {
		this.type = Types.JAVA_OBJECT;
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}
