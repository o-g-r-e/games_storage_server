package com.cm.dataserver.basedbclasses.queryclasses;

import java.util.ArrayList;
import java.util.List;

import com.cm.dataserver.basedbclasses.QueryTypedValue;

public class SimpleSqlExpression implements SqlExpression {
	private String name;
	private int type;
	private Object value;
	private String not;
	
	public SimpleSqlExpression(String name, int type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
		not = "";
	}
	
	public SimpleSqlExpression(String name, Object value) {
		this.name = name;
		this.value = value;
		not = "";
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
	
	public void setNot() {
		not = "NO";
	}
	
	@Override
	public String toString() {
		return ("NOT".equals(not)?"NOT ":"")+name+"=?";
	}
	
	@Override
	public List<QueryTypedValue> getTypedValues() {
		List<QueryTypedValue> result = new ArrayList<>();
		result.add(new QueryTypedValue(value));
		return result;
	}
}
