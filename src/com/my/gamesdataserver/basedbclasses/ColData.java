package com.my.gamesdataserver.basedbclasses;

public class ColData {
	private int type;
	private String name;
	private boolean isNull;
	private String defaultValue;
	
	public ColData(int type, String name) {
		this.type = type;
		this.name = name;
		this.isNull = true;
		this.defaultValue = null;
	}
	
	public ColData(int type, String name, boolean isNull) {
		this.type = type;
		this.name = name;
		this.isNull = isNull;
		this.defaultValue = null;
	}
	
	public ColData(int type, String name, String defaultValue) {
		this.type = type;
		this.name = name;
		this.isNull = true;
		this.defaultValue = defaultValue;
	}
	
	public ColData(int type, String name, boolean isNull, String defaultValue) {
		this.type = type;
		this.name = name;
		this.isNull = isNull;
		this.defaultValue = defaultValue;
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
}
