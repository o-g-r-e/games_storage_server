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
		
		return SqlMethods.insert(sqlInsert.toString(), toValueList(jsonValues), connection);
	}
	
	public static String generateWhere(JSONArray jsonWhere) throws JSONException {
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
	
	public static String generateUpdateSetPart(JSONArray jsonSet) throws JSONException {
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
	
	public static List<QueryTypedValue> toValueList(JSONArray jsonEntArray) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonEntArray.length(); i++) {
			Object el = jsonEntArray.get(i);
			if(el instanceof JSONArray) {
				JSONArray values = (JSONArray)el;
				for (int j = 0; j < values.length(); j++) {
					result.add(new QueryTypedValue(values.get(j)));
				}
			} else if(el instanceof JSONObject) {
				result.add(new QueryTypedValue(((JSONObject)el).get("value")));
			}
		}
		return result;
	}
	
	private static List<QueryTypedValue> parseJsonCondition(JSONArray jsonCondition) throws JSONException {
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
			sqlUpdate.append(generateWhere(jsonQuery.getJSONArray("condition")));
		}
		
		if(playerId != null) {
			sqlUpdate.append(" AND ").append(playerId.getFieldName()).append("=?");
		}
		
		List<QueryTypedValue> whereValues = parseJsonCondition(jsonQuery.getJSONArray("condition"));
		
		if(playerId != null) {
			whereValues.add(new QueryTypedValue(playerId.getValue()));
		}
		
		List<QueryTypedValue> resultValues = toValueList(jsonQuery.getJSONArray("set"));
		resultValues.addAll(whereValues);
		
		String sql = String.format(sqlUpdate.toString(), tableName, generateUpdateSetPart(jsonQuery.getJSONArray("set")));
		
		return SqlMethods.update(sql, resultValues, connection);
	}
	
	public static List<Row> select(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = tableNamePrefix+jsonQuery.getString("table");
		
		StringBuilder sql = new StringBuilder("SELECT %s FROM %s");
		
		sql.append(" WHERE ").append(playerId.getFieldName()).append("=?");
		
		if(jsonQuery.has("condition")) {
			sql.append(" AND ").append(generateWhere(jsonQuery.getJSONArray("condition")));
		}
		
		
		List<QueryTypedValue> resultValues = new ArrayList<>();
		resultValues.add(new QueryTypedValue(playerId.getValue()));
		
		if(jsonQuery.has("condition")) {
			resultValues.addAll(parseJsonCondition(jsonQuery.getJSONArray("condition")));
		}
		
		List<String> fieldsArray = new ArrayList<>();
		
		if(jsonQuery.has("fields")) {
			for(int i=0; i<jsonQuery.getJSONArray("fields").length();i++) {
				fieldsArray.add(jsonQuery.getJSONArray("fields").getString(i));
			}
		}
		
		String resultSql = String.format(sql.toString(), fieldsArray.size()>0?String.join(",", fieldsArray):"*", tableName);
		
		List<Row> result = SqlMethods.select(resultSql, resultValues, connection);
		
		for(Row row : result) {
			row.removeCell("id");
			row.removeCell(playerId.getFieldName());
		}
		
		return result;
	}
}
