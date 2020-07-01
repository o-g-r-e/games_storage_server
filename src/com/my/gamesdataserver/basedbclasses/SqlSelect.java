package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.SqlExpression.Cond;
import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;

public class SqlSelect {
	private String tableName;
	private List<String> fields;
	private SqlExpression where;

	public SqlSelect(String tableName) {
		this.tableName = tableName;
	}
	
	public SqlSelect(JSONObject jsonQuery) throws JSONException {
		fields = new ArrayList<>();
		tableName = jsonQuery.getString("table_name");
		
		if(!jsonQuery.isNull("fields")) {
			JSONArray jsonFields = jsonQuery.getJSONArray("fields");
			for (int i = 0; i < jsonFields.length(); i++) {
				fields.add(jsonFields.getString(i));
			}
		}
		
		if(!jsonQuery.isNull("condition")) {
			where = new SqlExpression(jsonQuery.getJSONArray("condition"));
		}
	}
	
	/*public SqlSelect withFields(String[] fields) {
		this.fields = new ArrayList<>();
		for(String f : fields) {
			this.fields.add(f);
		}
		return this;
	}*/
	
	public SqlSelect withFields(List<String> fields) {
		this.fields = fields;
		return this;
	}
	
	public SqlSelect setConditions(SqlExpression where) {
		this.where = where;
		return this;
	}

	public List<String> getFields() {
		return fields;
	}

	public SqlExpression getWhere() {
		return where;
	}

	public void addCondition(SqlWhereValue e) {
		where.addValue(e);
	}

	public String getTableName() {
		return tableName;
	}
}
