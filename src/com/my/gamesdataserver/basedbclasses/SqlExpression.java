package com.my.gamesdataserver.basedbclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.SqlExpression.Cond;
import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;

public class SqlExpression {
	
	private List<SqlWhereEntity> exps;
	
	public List<TypedValue> getTypedValues() {
		List<TypedValue> result = new ArrayList<>();
		for(SqlWhereEntity e : exps) {
			result.addAll(e.getTypedValues());
		}
		return result;
	}
	
	public String toPSFormat() {
		StringBuilder result = new StringBuilder();
		for (SqlWhereEntity e : exps) {
			result.append(e.toPsFormat());
		}
		return result.toString();
	}
	
	public enum Cond {
		AND {
	        public String toString() {
	            return "AND";
	        }
	    }, 
		OR{
	        public String toString() {
	            return "OR";
	        }
	    }
    }

	public int size() {
		return exps.size();
	};
	
	public List<SqlWhereEntity> getExpression() {
		return exps;
	}
	
	public void addValue(SqlWhereValue e) {
		exps.add(e);
	}
	
	public void addSet(SqlWhereSet e) {
		exps.add(e);
	}
	
	public SqlExpression(JSONArray jsonWhere) throws JSONException {
		for (int i = 0; i < jsonWhere.length(); i++) {
			JSONArray conditionLine = jsonWhere.getJSONArray(i);
			
			for (int j = 0; j < conditionLine.length(); j++) {
				JSONObject jsonWhereEntity = conditionLine.getJSONObject(j);
				
				int type = jsonWhereEntity.isNull("type") ? Types.NULL : DataBaseMethods.serverTypeToMysqType(jsonWhereEntity.getString("type"));
				SqlWhereValue exp = new SqlWhereValue(new CellData(type, jsonWhereEntity.getString("name"), jsonWhereEntity.getString("value")));
				
				if(j == 0 && i > 0) {
					exp.withCond(Cond.AND);
				} else if(j > 0) {
					exp.withCond(Cond.OR);
				}
				
				if(!jsonWhereEntity.isNull("is_not") && jsonWhereEntity.getBoolean("is_not")) {
					exp.not();
				}
				
				addValue(exp);
			}
		}
	}

	public SqlExpression() {
		// TODO Auto-generated constructor stub
	}
	
	/*private List<ExpEntity> expression;
	private List<Cond> logics;
	private StringBuilder psSqlFormat;
	
	public SqlExpression(ExpEntity e) {
		expression = new ArrayList<>();
		expression.add(e);
		psSqlFormat = new StringBuilder();
		psSqlFormat.append(e.getName()).append("=?");
	}
	
	public SqlExpression addInt(Cond logicExp, String name, int value) {
		add(logicExp, new ExpEntity<Integer>(name, value));
		return this;
	}
	
	public SqlExpression addFloat(Cond logicExp, String name, float value) {
		add(logicExp, new ExpEntity<Float>(name, value));
		return this;
	}
	
	public SqlExpression addString(Cond logicExp, String name, String value) {
		add(logicExp, new ExpEntity<String>(name, value));
		return this;
	}
	
	private void add(Cond logicExp, ExpEntity e) {
		expression.add(e);
		logics.add(logicExp);
		psSqlFormat.append(" ").append(logicExp.toString()).append(" ").append(e.getName()).append("=?");
	}

	*/
}
