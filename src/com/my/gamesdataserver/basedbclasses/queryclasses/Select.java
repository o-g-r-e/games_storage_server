package com.my.gamesdataserver.basedbclasses.queryclasses;

public class Select {
	private String table;
	private String felds;
	private SqlExpression where;
	private int limit;
	
	public Select(String table, String felds, SqlExpression where, int limit) {
		this.table = table;
		this.felds = felds;
		this.where = where;
		this.limit = limit;
	}
	
	
}
