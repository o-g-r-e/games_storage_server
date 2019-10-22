package com.my.gamesdataserver.rawdbclasses;

public class ColData {
	private int type;
	private String name;
	
	public ColData(int type, String name) {
		this.type = type;
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
