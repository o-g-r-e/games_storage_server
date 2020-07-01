package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.basedbclasses.SqlExpression.Cond;

public class SqlWhereSet implements SqlWhereEntity {
	private String name;
	private int type;
	private List<Object> values;
	private Cond cond;
	
	public SqlWhereSet(String name, int type) {
		this.name = name;
		this.type = type;
		values = new ArrayList<>();
	}
	
	@Override
	public String toPsFormat() {
		return new StringBuilder().append(" ").append(cond==null?"":cond.toString()).append(" ").append(name).append(" IN (").
				append(new String(new char[values.size()]).replace("\0", "?").replaceAll("\\?(?=\\?)", "?,")).append(")").toString();
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public List<Object> getValues() {
		return values;
	}
	
	public SqlWhereSet add(Object value) {
		values.add(value);
		return this;
	}

	@Override
	public List<TypedValue> getTypedValues() {
		List<TypedValue> result = new ArrayList<>();
		for(Object v : values) {
			result.add(new TypedValue(type, v));
		}
		
		return result;
	}
}
