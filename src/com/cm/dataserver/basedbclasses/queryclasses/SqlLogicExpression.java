package com.cm.dataserver.basedbclasses.queryclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.cm.dataserver.basedbclasses.QueryTypedValue;

public class SqlLogicExpression implements SqlExpression {
	private SqlExpression leftExp;
	private SqlExpression rightExp;
	private String condition;
	
	public SqlLogicExpression(SqlExpression leftExp, String condition, SqlExpression rightExp) {
		this.leftExp = leftExp;
		this.rightExp = rightExp;
		this.condition = condition;
	}
	
	@Override
	public String toString() {
		return leftExp.toString()+" "+condition+" "+rightExp.toString();
	}
	
	@Override
	public List<QueryTypedValue> getTypedValues() {
		List<QueryTypedValue> result = new ArrayList<>();
		result.addAll(leftExp.getTypedValues());
		result.addAll(rightExp.getTypedValues());
		
		return result;
	}
}
