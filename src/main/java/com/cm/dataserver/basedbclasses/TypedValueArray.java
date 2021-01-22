package com.cm.dataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

public class TypedValueArray {
	private List<QueryTypedValue> queryValues = new ArrayList<>();
	
	public TypedValueArray(Object ...args) {
		for (int i = 0; i < args.length; i++) {
			queryValues.add(new QueryTypedValue(args[i]));
		}
	}

	public List<QueryTypedValue> getQueryValues() {
		return queryValues;
	}
}
