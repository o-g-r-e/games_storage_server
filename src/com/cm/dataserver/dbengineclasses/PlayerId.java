package com.cm.dataserver.dbengineclasses;

public class PlayerId {
	private String fieldName;
	private String value;
	
	public PlayerId(String fieldName, String value) {
		this.fieldName = fieldName;
		this.value = value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getValue() {
		return value;
	}
}
