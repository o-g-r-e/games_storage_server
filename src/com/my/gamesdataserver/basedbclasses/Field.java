package com.my.gamesdataserver.basedbclasses;

public class Field {
	private int type;
	private String name;
	private boolean isNull;
	private String defaultValue;
	private boolean autoIncrement;
	
	public Field(int type, String name) {
		this.type = type;
		this.name = name;
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
}