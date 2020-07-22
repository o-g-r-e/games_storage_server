package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;

public class Field {
	private int type;
	private String name;
	private int length;
	private boolean isNull;
	private String defaultValue;
	private boolean autoIncrement;
	
	public Field(int type, String name) {
		this.type = type;
		this.name = name;
		this.length = 45;
		this.isNull = true;
		this.defaultValue = null;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public boolean isNull() {
		return isNull;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public int getLength() {
		return length;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public Field setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
		return this;
	}

	public Field setNull(boolean isNull) {
		this.isNull = isNull;
		return this;
	}

	public Field setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public Field setLength(int length) {
		this.length = length;
		return this;
	}
	
	private String typeToString(int type) {
		switch (type) {
		case Types.INTEGER:
			return "INT";
		case Types.VARCHAR:
			return "VARCHAR("+length+")";
		case Types.FLOAT:
			return "FLOAT";
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("`").append(name).append("` ").append(typeToString(type)).append(isNull?" NULL":" NOT NULL").append(autoIncrement?" AUTO_INCREMENT":"").append(defaultValue!=null?" DEFAULT "+defaultValue:"");
		
		
		return result.toString();
	}
}