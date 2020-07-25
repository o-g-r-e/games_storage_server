package com.my.gamesdataserver.dbengineclasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.QueryTypedValue;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.queryclasses.Select;
import com.my.gamesdataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.my.gamesdataserver.basedbclasses.queryclasses.SqlExpression;
import com.my.gamesdataserver.basedbclasses.queryclasses.SqlLogicExpression;

public class ApiMethods {
	
	public static int insert(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = jsonQuery.getString("table");
		
		JSONArray jsonValues = jsonQuery.getJSONArray("values");
		
		StringBuilder sqlInsert = new StringBuilder("INSERT INTO ").append(tableNamePrefix+tableName);
		
		if(jsonQuery.has("fields") && jsonQuery.getJSONArray("fields").length() > 0) {
			sqlInsert.append(" (id,").append(playerId.getFieldName()).append(",").append(jsonQuery.getJSONArray("fields").join(", ")).append(")");
		}
		
		sqlInsert.append(" VALUES ");
		
		String values = "("+new String(new char[jsonValues.getJSONArray(0).length()]).replace("\0", "?").replaceAll("\\?(?=\\?)", "?,")+")";
		values += new String(new char[jsonValues.length()-1]).replace("\0", values);
		values = values.replaceAll("\\)\\(", "),(");
		
		
		values = values.replaceAll("\\(", "(DEFAULT,?,");
		for (int i = 0; i < jsonValues.length(); i++) {
			JSONArray jsonRow = jsonValues.getJSONArray(i);
			jsonValues.put(i, new JSONArray("['"+playerId.getValue()+"',"+jsonRow.join(",")+"]"));
		}
		
		sqlInsert.append(values);
		
		return SqlMethods.insert(sqlInsert.toString(), getValuesFromInsert(jsonValues), connection);
	}
	
	public static String jsonWhereToSql(JSONArray jsonWhere) throws JSONException {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < jsonWhere.length(); i++) {
			JSONArray andJsonExpression = jsonWhere.getJSONArray(i);
			
			if(i > 0) {
				result.append(" OR ");
			}
			
			for (int j = 0; j < andJsonExpression.length(); j++) {
				JSONObject jsonWhereExpression = andJsonExpression.getJSONObject(j);

				if(j > 0) {
					result.append(" AND ");
				}
				
				if(jsonWhereExpression.has("is_not")) {
					result.append(" NOT ");
				}
				
				result.append(jsonWhereExpression.getString("name")).append("=?");
			}
		}
		return result.toString();
	}
	
	private static String jsonUpdateSetToSql(JSONArray jsonSet) throws JSONException {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < jsonSet.length(); i++) {
			JSONObject jsonSetExpression = jsonSet.getJSONObject(i);
			
			if(i > 0) {
				result.append(", ");
			}
			
			result.append(jsonSetExpression.getString("name")).append("=?");
		}
		return result.toString();
	}
	
	private static List<QueryTypedValue> getValuesFromInsert(JSONArray jsonInsertValues) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonInsertValues.length(); i++) {
			JSONArray values = jsonInsertValues.getJSONArray(i);
			for (int j = 0; j < values.length(); j++) {
				result.add(new QueryTypedValue(values.get(j)));
			}
		}
		return result;
	}
	
	private static List<QueryTypedValue> getValuesFromUpdateSet(JSONArray jsonSet) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonSet.length(); i++) {
			result.add(new QueryTypedValue(jsonSet.getJSONObject(i).get("value")));
		}
		return result;
	}
	
	private static List<QueryTypedValue> getValuesFromWhere(JSONArray jsonCondition) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonCondition.length(); i++) {
			JSONArray andExpressions = jsonCondition.getJSONArray(i);
			for (int j = 0; j < andExpressions.length(); j++) {
				JSONObject jsonExpression = andExpressions.getJSONObject(j);
				result.add(new QueryTypedValue(jsonExpression.get("value")));
			}
		}
		return result;
	}
	
	public static int update(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = tableNamePrefix+jsonQuery.getString("table");
		
		StringBuilder sqlUpdate = new StringBuilder("UPDATE %s SET %s");
		
		if(jsonQuery.has("condition") || playerId != null) {
			sqlUpdate.append(" WHERE ");
		}
		
		if(jsonQuery.has("condition")) {
			sqlUpdate.append(jsonWhereToSql(jsonQuery.getJSONArray("condition")));
		}
		
		if(playerId != null) {
			sqlUpdate.append(" AND ").append(playerId.getFieldName()).append("=?");
		}
		
		List<QueryTypedValue> whereValues = getValuesFromWhere(jsonQuery.getJSONArray("condition"));
		
		if(playerId != null) {
			whereValues.add(new QueryTypedValue(playerId.getValue()));
		}
		
		List<QueryTypedValue> resultValues = getValuesFromUpdateSet(jsonQuery.getJSONArray("set"));
		resultValues.addAll(whereValues);
		
		String sql = String.format(sqlUpdate.toString(), tableName, jsonUpdateSetToSql(jsonQuery.getJSONArray("set")));
		
		return SqlMethods.update(sql, resultValues, connection);
	}
	
	public static List<Row> select(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = tableNamePrefix+jsonQuery.getString("table");
		
		StringBuilder sql = new StringBuilder("SELECT %s FROM %s");
		
		sql.append(" WHERE ").append(playerId.getFieldName()).append("=?");
		
		if(jsonQuery.has("condition")) {
			sql.append(" AND ").append(jsonWhereToSql(jsonQuery.getJSONArray("condition")));
		}
		
		List<QueryTypedValue> resultValues = new ArrayList<>();
		resultValues.add(new QueryTypedValue(playerId.getValue()));
		
		if(jsonQuery.has("condition")) {
			resultValues.addAll(getValuesFromWhere(jsonQuery.getJSONArray("condition")));
		}
		
		String resultSql = String.format(sql.toString(), jsonQuery.has("fields")?jsonQuery.getJSONArray("fields").join(","):"*", tableName);
		
		List<Row> result = SqlMethods.select(resultSql, resultValues, connection);
		
		for(Row row : result) {
			row.removeCell("id");
			row.removeCell(playerId.getFieldName());
		}
		
		return result;
	}
	
	public static List<Row> select(Select select, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		SqlExpression where = select.getWhere();
		SimpleSqlExpression playerIdWhereExpression = new SimpleSqlExpression(playerId.getFieldName(), playerId.getValue());
		
		if(where == null) {
			where = playerIdWhereExpression;
		} else {
			where = new SqlLogicExpression(playerIdWhereExpression, "AND", where);
		}
		
		select.setWhere(where);
		select.setTable(tableNamePrefix+select.getTable());
		
		List<Row> result = SqlMethods.select(select.toString(), where.getTypedValues(), connection);
		
		for(Row row : result) {
			row.removeCell("id");
			row.removeCell(playerId.getFieldName());
		}
		
		return result;
	}
}
