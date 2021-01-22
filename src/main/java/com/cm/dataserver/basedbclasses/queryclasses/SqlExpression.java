package com.cm.dataserver.basedbclasses.queryclasses;

import java.util.List;

import com.cm.dataserver.basedbclasses.QueryTypedValue;

public interface SqlExpression {
	public List<QueryTypedValue> getTypedValues();
}
