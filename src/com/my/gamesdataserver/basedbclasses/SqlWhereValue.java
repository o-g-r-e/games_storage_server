package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.basedbclasses.SqlExpression.Cond;

public class SqlWhereValue implements SqlWhereEntity {
	private CellData core;
	private boolean not;
	private Cond cond;
	
	public SqlWhereValue(CellData core) {
		this.core = core;
		this.not = false;
		this.cond = null;
	}
	
	@Override
	public String toPsFormat() {
		return new StringBuilder().append(" ").append(cond==null?"":cond.toString()).append(" ").append(core.getName()).append("=?").toString();
	}
	
	public SqlWhereValue not() {
		not = true;
		return this;
	}
	
	public SqlWhereValue withCond(Cond c) {
		cond = c;
		return this;
	}
	public CellData getCore() {
		return core;
	}
	public boolean isNot() {
		return not;
	}
	public Cond getCond() {
		return cond;
	}

	@Override
	public List<TypedValue> getTypedValues() {
		List<TypedValue> result = new ArrayList<>();
		result.add(new TypedValue(core.getType(), core.getValue()));
		return result;
	}
}