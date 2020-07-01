package com.my.gamesdataserver.basedbclasses;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.DatabaseConnectionManager;
import com.my.gamesdataserver.DatabaseConnectionPoolC3P0;
import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;

public class SqlMethods {
	
	private static boolean printQueries = false;
	
	public static void createTable(String name, Field[] fields, String primaryKey, Connection connection) throws SQLException {
		StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS "+name+" (");
		
		for (int i = 0; i < fields.length; i++) {
			query.append(fields[i].toString());
			
			if(i < fields.length-1) {
				query.append(", ");
			}
		}
		
		if(primaryKey != null) {
			query.append(", PRIMARY KEY (`"+primaryKey+"`)");
		}
		
		query.append(");");
		
		connection.prepareStatement(query.toString()).execute();
	}
	
	public static int insert(String tableName, List<Row> rows, Connection connection) throws SQLException {
		String values = new String(new char[rows.get(0).size()]).replace("\0", "?").replaceAll("\\?(?=\\?)", "?,");
		//String values = new String(new char[row.size()]).replace("\0", "?,").substring(0, (row.size()*2)-1);
		StringBuilder sqlRows = new StringBuilder();
		for(Row r : rows) {
			sqlRows.append("(").append(values).append(")");
		}
		
		PreparedStatement pstmt = connection.prepareStatement(String.format("INSERT INTO %s (%s) VALUES %s", tableName, String.join(",", rows.get(0).cellNames()), sqlRows.toString().replaceAll("\\)\\(", "),(")));
		int i = 1;
		for(Row r : rows) {
			for(CellData e : r.getCells()) {
				setQueryValue(pstmt, e.getType(), e.getValue(), i++);
			}
		}
		
		return pstmt.executeUpdate();
	}
	
	/*public static int insert(String tableName, String row, Connection connection) throws SQLException {
		return insert(tableName, parseValuesListLiteral(row), connection);
	}*/
	
	public static int updateTable(String tableName, Row set, SqlExpression where, Connection connection) throws SQLException {
		StringBuilder sqlUpdate = new StringBuilder("UPDATE %s SET %s");
		StringBuilder sqlSet = new StringBuilder();
		
		for(CellData e : set.getCells()) {
			sqlSet.append(e.getName()).append("=?");
		}
		
		if(where != null && where.size() > 0) {
			sqlUpdate.append(" WHERE ").append(where.toPSFormat());
		}
		
		if(printQueries) {
			System.out.println(sqlUpdate.toString());
		}
		
		PreparedStatement pstmt = connection.prepareStatement(String.format(sqlUpdate.toString(), tableName, Pattern.compile("\\=\\?(?=[^$])").matcher(sqlSet).replaceAll("=?,")));
		
		int i = 1;
		for(CellData e : set.getCells()) {
			setQueryValue(pstmt, e.getType(), e.getValue(), i++);
		}
		
		List<TypedValue> typedValues = where.getTypedValues();
		
		for (TypedValue tv : typedValues) {
			setQueryValue(pstmt, tv.getType(), tv.getValue(), i+1+set.size());
		}
		
		return pstmt.executeUpdate();
	}
	
	/*public static int updateTable(String tableName, String set, String where, Connection connection) throws SQLException {
		return updateTable(tableName, parseValuesListLiteral(set), parseValuesListLiteral(where), connection);
	}*/
	
	public static List<Row> select(String tableName, String fields, SqlExpression where, boolean first, Connection connection) throws SQLException {
		StringBuilder sql = new StringBuilder("SELECT ").append(fields).append(" FROM ").append(tableName);
		if(where != null && where.getExpression().size() > 0) {
			sql.append(" WHERE ").append(where.toPSFormat());
		}
		
		if(first) {
			sql.append(" LIMIT 1");
		}
		
		if(printQueries) {
			System.out.println(sql.toString());
		}
		
		PreparedStatement pstmt = connection.prepareStatement(sql.toString());
		
		List<TypedValue> typedValues = where.getTypedValues();
		
		int i = 0;
		for (TypedValue tv : typedValues) {
			setQueryValue(pstmt, tv.getType(), tv.getValue(), i++);
		}
		
		ResultSet resultSet = pstmt.executeQuery();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		List<Row> result = new ArrayList<>();
		
		while(resultSet.next()) {
			Row row = new Row();
			for (i = 1; i <= rsmd.getColumnCount(); i++) {
				row.addCell(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(row);
		}
		return result;
	}
	
	public static List<Row> select(String tableName, boolean first, Connection connection) throws SQLException {
		return select(tableName, "*", null, first, connection);
	}
	
	public static List<Row> select(String tableName, SqlExpression where, boolean first, Connection connection) throws SQLException {
		return select(tableName, "*", where, first, connection);
	}
	
	/*private static List<SqlExpression> parseValuesListLiteral(String value) {
		List<SqlExpression> whereData = new ArrayList<>();
		Map<String, String> wherePairs = new HashMap<>();
		
		String[] pairs = value.split("&");
		
		if(pairs.length >= 2 || value.contains("=")) {
			for(String pair : pairs) {
				String[] params = pair.split("=");
				
				if(params.length < 2) {
					continue;
				}
				wherePairs.put(params[0], params[1]);
			}
		}
		
		for(Map.Entry<String, String> e : wherePairs.entrySet()) {
			whereData.add(new SqlExpression(0, e.getKey(), e.getValue()));
		}
		
		return whereData;
	}*/
	
	/*private static String buildWhere(List<SqlExpression> where) {
		if(where == null || where.size() <= 0) {
			return "";
		}
		
		StringBuilder result = new StringBuilder(" WHERE ");
		
		for (int i = 0; i < where.size(); i++) {
			SqlExpression exp = where.get(i);
			if(exp.getValue() instanceof Object[]) {
				Object[] objects = (Object[]) exp.getValue();
				result.append(exp.getName()).append(" IN (");
				
				for (int j = 0; j < objects.length; j++) {
					
					result.append("?");
					if(j < objects.length-1) {
						result.append(",");
					}
				}
				result.append(")");
			} else {
				result.append(exp.getName()).append("=?");
			}
			
			if(i < where.size()-1) {
				result.append(" AND ");
			}
		}
		
		return result.toString();
	}*/
	
	public static int parseDataType(String type) {
		switch (type) {
		case "INTEGER":
			return Types.INTEGER;
		case "STRING":
			return Types.VARCHAR;
		case "FLOAT":
			return Types.FLOAT;
		}
		return -1;
	}
	
	private static void setQueryValue(PreparedStatement pstmt, int type, Object value, int index) throws SQLException {
		switch (type) {
		case Types.INTEGER:
			pstmt.setInt(index, (int)value);
			break;
		case Types.VARCHAR:
			pstmt.setString(index, String.valueOf(value));
			break;
		case Types.FLOAT:
			pstmt.setFloat(index, (float)value);
			break;
		default:
			pstmt.setObject(index, value);
			break;
		}
	}
	
	public static String[] findTablesByPrefix(String databaseName, String tableNamePrefix, Connection connection) throws SQLException {
		PreparedStatement pstmt = connection.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '?' AND TABLE_NAME LIKE '?%'");
		pstmt.setString(1, databaseName);
		pstmt.setString(2, tableNamePrefix);
		ResultSet resultSet = pstmt.executeQuery();
		List<String> tableNames = new ArrayList<>();
		while(resultSet.next()) {
			tableNames.add(resultSet.getString(1));
		}
		return tableNames.toArray(new String[] {});
	}
	
	public static List<Row> executeSelect(SqlSelect sqlSelect, boolean first, Connection connection) throws SQLException {
		if(sqlSelect.getFields() != null) {
			return select(sqlSelect.getTableName(), String.join(",", sqlSelect.getFields()), sqlSelect.getWhere(), first, connection);
		}
		return select(sqlSelect.getTableName(), sqlSelect.getWhere(), first, connection);
	}
	
	public static int executeInsert(SqlInsert sqlInsert, boolean first, Connection connection) throws SQLException {
		return insert(sqlInsert.getTableName(), sqlInsert.getRows(), connection);
	}
	
	public static int executeUpdate(SqlUpdate sqlUpdate, boolean first, Connection connection) throws SQLException {
		return updateTable(sqlUpdate.getTableName(), sqlUpdate.getSet(), sqlUpdate.getWhere(), connection);
	}
	
	/*public static List<SqlExpression> parseWhere(JSONArray whereData) throws JSONException {
		List<SqlExpression> result = new ArrayList<>();*/
		//JSONArray whereData = new JSONArray(jsonArray);
		/*for (int i = 0; i < whereData.length(); i++) {
			JSONObject cellObject = whereData.getJSONObject(i);
			if(!cellObject.has("name") || !cellObject.has("value")) {
				continue;
			}
			
			Object value = null;
			
			if(cellObject.get("value") instanceof JSONArray) {
				JSONArray values = cellObject.getJSONArray("value");
				Object[] objects = new Object[values.length()];
				for (int j = 0; j < values.length(); j++) {
					objects[j] = values.get(j);
				}
				value = objects;
			} else {
				value = cellObject.getString("value");
			}
			
			SqlExpression cd = new SqlExpression(cellObject.getString("name"), value);
			
			if(cellObject.has("type")) {
				cd.setType(SqlMethods.parseDataType(cellObject.getString("type")));
			} 
			
			result.add(cd);
		}
		return result;
	}*/
	
	/*public static List<SqlExpression> parseCellDataRow(String jsonCallDataArray) throws JSONException {
		return parseCellDataRow(new JSONArray(jsonCallDataArray));
	}
	
	public static List<List<SqlExpression>> parseCellDataRows(String jsonCallDataArray) throws JSONException {
		return parseCellDataRows(new JSONArray(jsonCallDataArray));
	}*/
	
	/*public static List<SqlExpression> parseCellDataRow(JSONArray rowArray) throws JSONException {
		List<SqlExpression> result = new ArrayList<>();
		for (int i = 0; i < rowArray.length(); i++) {
			JSONObject cellObject = rowArray.getJSONObject(i);
			if(!cellObject.has("name") || !cellObject.has("value")) {
				continue;
			}
			if(cellObject.has("type")) {
				result.add(new SqlExpression(SqlMethods.parseDataType(cellObject.getString("type")), cellObject.getString("name"), cellObject.getString("value")));
			} else {
				result.add(new SqlExpression(cellObject.getString("name"), cellObject.getString("value")));
			}
		}
		return result;
	}*/
	
	/*public static List<List<SqlExpression>> parseCellDataRows(JSONArray rowsArray) throws JSONException {
		List<List<SqlExpression>> result = new ArrayList<>();
		for (int i = 0; i < rowsArray.length(); i++) {
			result.add(parseCellDataRow(rowsArray.getJSONArray(i)));
		}
		return result;
	}*/
	
	public static void createIndex(String indexName, String tableName, String[] fields, boolean unique, Connection connection) throws SQLException {
		connection.prepareStatement(String.format("CREATE %s INDEX %s ON %s(%s)", unique?"UNIQUE":"", indexName, tableName, String.join(",", fields))).execute();
	}

	/*public boolean executeIncrement(Increment increment) throws SQLException, JSONException {
		String tableName = increment.getTableName();
		String fieldToIncrement = increment.getFieldName();
		List<SqlExpression> whereExpression = increment.getWhereExpression();
		List<List<CellData>> rows = selectAllWhere(tableName, whereExpression);
		int result = 0;
		if(rows.size() > 0) {
			CellData cellToInc = new Row(rows.get(0)).getCell(fieldToIncrement);
			if(cellToInc != null) {
				cellToInc.setValue(((Integer)cellToInc.getValue())+1);
				List<CellData> set = new ArrayList<>();
				set.add(cellToInc);
				result = updateTable(tableName, set, whereExpression);
			}
		}
		return result > 0;
	}*/
	
	/*public void execute(List<SqlRequest> requests, Connection connection) throws SQLException {
		for (SqlRequest sqlRequest : requests) {
			if(sqlRequest instanceof SqlInsert) {
				int result = executeInsert((SqlInsert) sqlRequest, connection);
				if(result <= 0) {
					
				}
			} else if(sqlRequest instanceof SqlUpdate) {
				int result = DataBaseMethods.executeUpdate((SqlUpdate) sqlRequest, connection);
				if(result <= 0) {
					
				}
			}
		}
	}*/
}
