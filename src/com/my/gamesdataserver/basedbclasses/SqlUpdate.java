package com.my.gamesdataserver.basedbclasses;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.SqlExpression;

public class SqlUpdate extends SqlRequest {
	private List<SqlExpression> updatesData = new ArrayList<>();
	
	public SqlUpdate(String tableName, List<SqlExpression> whereData, List<SqlExpression> updatesData) throws JSONException {
		super(tableName, whereData);
		this.updatesData = updatesData;
	}

	public List<SqlExpression> getUpdatesData() {
		return updatesData;
	}
}
