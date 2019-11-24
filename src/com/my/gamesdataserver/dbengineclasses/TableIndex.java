package com.my.gamesdataserver.dbengineclasses;

public class TableIndex {
	private String name;
	private String[] fields;
	private boolean unique;
	
	public TableIndex(String name, String[] fields, boolean unique) {
		this.name = name;
		this.fields = fields;
		this.unique = unique;
	}

	public String getName() {
		return name;
	}

	public String[] getFields() {
		return fields;
	}

	public boolean isUnique() {
		return unique;
	}
}
