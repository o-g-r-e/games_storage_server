package com.my.gamesdataserver.basedbclasses;

public class ColData {
	private int type;
	private String name;
	private boolean isNull;
	private String defaultValue;
	private boolean autoIncrement;
	
	public ColData(int type, String name) {
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

	public ColData setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
		return this;
	}

	public ColData setNull(boolean isNull) {
		this.isNull = isNull;
		return this;
	}

	public ColData setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	
}
