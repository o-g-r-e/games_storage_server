package com.my.gamesdataserver.basedbclasses;

import java.util.List;

public interface SqlWhereEntity {
	public String toPsFormat();
	public List<TypedValue> getTypedValues();
}
