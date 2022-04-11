package com.cm.dataserver.basedbclasses;

public class ForeignKey {
	private String keyField;
	private String foreignTableName;
	private String foreignFieldName;

	public ForeignKey(String keyField, String foreignTableName, String foreignFieldName) {
		this.keyField = keyField;
		this.foreignTableName = foreignTableName;
		this.foreignFieldName = foreignFieldName;
	}

    public String getKeyField() {
        return keyField;
    }

    public String getForeignTableName() {
        return foreignTableName;
    }

    public String getForeignFieldName() {
        return foreignFieldName;
    }
}