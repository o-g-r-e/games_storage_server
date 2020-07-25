package com.my.gamesdataserver.basedbclasses.queryclasses;

import java.util.List;

import com.my.gamesdataserver.basedbclasses.QueryTypedValue;

public interface SqlExpression {
	public List<QueryTypedValue> getTypedValues();
}
