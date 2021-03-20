package com.cm.dataserver.dbengineclasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.basedbclasses.CellData;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TypedValueArray;
import com.cm.dataserver.basedbclasses.queryclasses.Select;
import com.cm.dataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.cm.dataserver.basedbclasses.queryclasses.SqlExpression;
import com.cm.dataserver.basedbclasses.queryclasses.SqlLogicExpression;
import com.cm.dataserver.template1classes.LifeRequest;

public class ApiMethods {
	
	private static Pattern helpPattern = Pattern.compile("\"([\\w\"]+)\"");
	
	private static String fieldsWithoutQuotes(JSONArray jsonArray) throws JSONException {
		Matcher m = helpPattern.matcher(jsonArray.join(","));
		String[] resultValues = new String[jsonArray.length()];
		
		int i=0;
		while (m.find()) 
			resultValues[i++] = m.group(1);
		
		return String.join(",", resultValues);
	}
	
	/*private static String questionMarks(int count) {
		if(count <= 0) return "";
		return new String(new char[count]).replace("\0", "?").replaceAll("\\?(?=\\?)", "?,");
	}*/
	
	/*private static String valuesArea(int valuesCount, int insertRowsCount) {
		String firstRowResult = questionMarks(valuesCount);
		
		if("".equals(firstRowResult)) {
			return "";
		}
		
		String values = "("+questionMarks(valuesCount)+")";*/
		/*values += new String(new char[jsonValues.length()-1]).replace("\0", values);
		values = values.replaceAll("\\)\\(", "),(");*/
		/*StringBuilder result = new StringBuilder((values.length()*insertRowsCount)+insertRowsCount-1);
		result.append(values);
		for (int i = 1; i < insertRowsCount; i++) {
			result.append(",").append(values);
		}
		return result.toString();
	}*/
	
	public static int insert(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = jsonQuery.getString("table");
		
		JSONArray jsonValues = jsonQuery.getJSONArray("values");
		
		StringBuilder sqlInsert = new StringBuilder("INSERT INTO ").append(tableNamePrefix+tableName);
		
		if(jsonQuery.has("fields") && jsonQuery.getJSONArray("fields").length() > 0) {
			sqlInsert.append(" (id,")
			.append(playerId.getFieldName())
			.append(",")
			.append(fieldsWithoutQuotes(jsonQuery.getJSONArray("fields")))
			.append(")");
		}
		
		sqlInsert.append(" VALUES ");
		String values = "(DEFAULT,?"+StringDataHelper.repeat(",?",jsonValues.getJSONArray(0).length())+")";
		values += StringDataHelper.repeat(","+values, jsonValues.length()-1);
		sqlInsert.append(values);
		/*String values = "("+questionMarks(jsonValues.getJSONArray(0).length())+")";
		values += new String(new char[jsonValues.length()-1]).replace("\0", values);
		values = values.replaceAll("\\)\\(", "),(");*/
		//String valuesQueryArea = valuesArea(jsonValues.getJSONArray(0).length(), jsonValues.length());
		
		
		//valuesQueryArea = valuesQueryArea.replaceAll("\\(", "(DEFAULT,?,"); //DEFAULT - auto increment id, ? - player_id
		for (int i = 0; i < jsonValues.length(); i++) {
			JSONArray jsonRow = jsonValues.getJSONArray(i);
			jsonValues.put(i, new JSONArray("['"+playerId.getValue()+"',"+jsonRow.join(",")+"]"));
		}
		
		//sqlInsert.append(valuesQueryArea);
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
		
		StringBuilder sqlUpdate = new StringBuilder("UPDATE %s SET %s WHERE ");
		
		if(playerId == null) {
			return 0;
		}
		
		List<QueryTypedValue> whereValues = new ArrayList<>();
		
		if(jsonQuery.has("condition") && jsonQuery.getJSONArray("condition").length() > 0) {
			sqlUpdate.append(jsonWhereToSql(jsonQuery.getJSONArray("condition")));
			whereValues = getValuesFromWhere(jsonQuery.getJSONArray("condition"));
			sqlUpdate.append(" AND ");
		}
		
		if(playerId != null) {
			sqlUpdate.append(playerId.getFieldName()).append("=?");
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
		
		String resultSql = String.format(sql.toString(), jsonQuery.has("fields")?fieldsWithoutQuotes(jsonQuery.getJSONArray("fields")):"*", tableName);
		
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
