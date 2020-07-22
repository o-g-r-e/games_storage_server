package com.my.gamesdataserver.basedbclasses.queryclasses;

public class SqlLogicExpression implements SqlExpression {
	private SqlExpression leftExp;
	private SqlExpression rightExp;
	private String condition;
	
	@Override
	public String toString() {
		return leftExp.toString()+condition+rightExp.toString();
	}
}
