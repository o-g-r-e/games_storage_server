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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.DatabaseConnectionManager;
import com.my.gamesdataserver.DatabaseConnectionPoolC3P0;
import com.my.gamesdataserver.SqlExpression;

public class SqlMethods {
	
	private static boolean printQueries = false;
	
	/*private boolean transaction = false;
	
	private DatabaseConnection databaseConnection;
	

	public DataBaseInterface(DatabaseConnection databaseConnection) throws SQLException {
		this.databaseConnection = databaseConnection;
	}
	
	public void enableTransactions() throws SQLException {
		databaseConnection.getConnection().setAutoCommit(false);
		transaction = true;
	}
	
	public void disableTransactions() throws SQLException {
		databaseConnection.getConnection().setAutoCommit(true);
		transaction = false;
	}
	
	public void commit() throws SQLException {
		databaseConnection.getConnection().commit();
	}
	
	public void rollback() throws SQLException {
		databaseConnection.getConnection().rollback();
	}*/
	
	//public boolean isTransactionsEnabled() throws SQLException {
		////return getCon().getAutoCommit();
		//return transaction;
	//}
	
	public static void deleteFrom(String tableName, String where, Connection connection) throws SQLException {
		
		if(where.contains("&")) {
			where = where.replaceAll("&", " AND ");
		}
		
		connection.prepareStatement("DELETE FROM `"+tableName+"` WHERE ("+where+")").execute();
	}
	
	public static void createTable(String name, Field[] fields, String primaryKey, Connection connection) throws SQLException {
		StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS "+name+" (");
		
		for (int i = 0; i < fields.length; i++) {
			query.append("`").append(fields[i].getName()).append("` ");
			switch (fields[i].getType()) {
			case Types.INTEGER:
				query.append("INT");
				break;
			case Types.VARCHAR:
				query.append("VARCHAR(45)");
				break;
			case Types.FLOAT:
				query.append("FLOAT");
				break;
			}
			
			if(fields[i].isNull()) {
				query.append(" NULL");
			} else {
				query.append(" NOT NULL");
			}
			
			if(fields[i].isAutoIncrement()) {
				query.append(" AUTO_INCREMENT");
			}
			
			if(fields[i].getDefaultValue() != null) {
				query.append(" DEFAULT ");
				if(fields[i].getType() == Types.VARCHAR) {
					query.append("'").append(fields[i].getDefaultValue()).append("'");
				} else if(fields[i].getType() == Types.INTEGER) {
					query.append(fields[i].getDefaultValue());
				}
			}
			
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
	
	public static void dropTable(String tableName, Connection connection) throws SQLException {
		connection.prepareStatement("DROP TABLE `"+tableName+"`").execute();
	}
	
	public static int insertIntoTable(String tableName, List<SqlExpression> row, Connection connection) throws SQLException {
		StringBuilder fieldsNames = new StringBuilder("(");
		StringBuilder fieldsValues = new StringBuilder("(");
		int width = row.size();
		for (int i = 0; i < width; i++) {
			SqlExpression d = row.get(i);
			fieldsNames.append(d.getName());
			fieldsValues.append("?");
			if(i < width-1) {
				fieldsNames.append(",");
				fieldsValues.append(",");
			}
		}
		
		fieldsNames.append(")");
		fieldsValues.append(")");
		
		PreparedStatement pstmt = connection.prepareStatement(String.format("INSERT INTO %s %s VALUES %s", tableName, fieldsNames, fieldsValues));
		
		for (int i = 0; i < row.size(); i++) {
			setQueryValue(pstmt, row.get(i).getType(), row.get(i).getValue(), i+1);
		}
		
		return pstmt.executeUpdate();
	}
	
	public static int insertIntoTable(String tableName, String row, Connection connection) throws SQLException {
		return insertIntoTable(tableName, parseValuesListLiteral(row), connection);
	}
	
	public static int insertIntoTableMultiRows(String tableName, List<List<SqlExpression>> row) throws SQLException {
		return 0;
	}
	
	public static int updateTable(String tableName, List<SqlExpression> set, List<SqlExpression> where, Connection connection) throws SQLException {
		StringBuilder sqlUpdate = new StringBuilder("UPDATE ");
		sqlUpdate.append(tableName);
		sqlUpdate.append(" SET ");
		for (int i = 0; i < set.size(); i++) {
			sqlUpdate.append(set.get(i).getName());
			sqlUpdate.append("=?");
			if(i < set.size()-1) {
				sqlUpdate.append(",");
			}
		}
		sqlUpdate.append(" WHERE (");
		for (int i = 0; i < where.size(); i++) {
			SqlExpression exp = where.get(i);
			sqlUpdate.append(exp.getName());
			sqlUpdate.append("=?");
			if(i < where.size()-1) {
				sqlUpdate.append(" AND ");
			}
		}
		sqlUpdate.append(")");
		
		if(printQueries) {
			System.out.println(sqlUpdate.toString());
		}
		
		PreparedStatement pstmt = connection.prepareStatement(sqlUpdate.toString());//con.prepareStatement(String.format("UDPATE %s SET %s WHERE %s", tableName, ));
		
		for (int i = 0; i < set.size(); i++) {
			setQueryValue(pstmt, set.get(i).getType(), set.get(i).getValue(), i+1);
		}
		
		for (int i = 0; i < where.size(); i++) {
			setQueryValue(pstmt, where.get(i).getType(), where.get(i).getValue(), i+1+set.size());
		}
		
		return pstmt.executeUpdate();
	}
	
	public static int updateTable(String tableName, String set, String where, Connection connection) throws SQLException {
		return updateTable(tableName, parseValuesListLiteral(set), parseValuesListLiteral(where), connection);
	}
	
	public static List<Row> select(String tableName, List<String> fields, List<SqlExpression> where, Connection connection) throws SQLException {
		StringBuilder sql = new StringBuilder("SELECT ");
		if(fields == null || fields.size() <= 0) {
			sql.append("*");
		} else {
			sql.append(String.join(", ", fields));
		}
		sql.append(" FROM ").append(tableName);
		if(where != null) {
			sql.append(buildWhere(where));
		}
		List<Row> result = new ArrayList<>();
		
		if(printQueries) {
			System.out.println(sql.toString());
		}
		
		PreparedStatement pstmt = connection.prepareStatement(sql.toString());
		int requestValueIndex = 0;
		int expIndex = 0;
		for (SqlExpression exp : where) {
			if(exp.getValue() instanceof Object[]) {
				Object[] valueSet = (Object[]) exp.getValue();
				for(int i=0;i<valueSet.length;i++) {
					setQueryValue(pstmt, where.get(expIndex).getType(), valueSet[i], requestValueIndex+1);
					requestValueIndex++;
				}
			} else {
				setQueryValue(pstmt, where.get(expIndex).getType(), where.get(expIndex).getValue(), requestValueIndex+1);
				requestValueIndex++;
			}
			expIndex++;
		}
		ResultSet resultSet = pstmt.executeQuery();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		while(resultSet.next()) {
			List<CellData> row = new ArrayList<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				row.add(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(new Row(row));
		}
		return result;
	}
	
	public List<Row> selectAll(String tableName, Connection connection) throws SQLException {
		return select(tableName, null, null, connection);
	}
	
	public static List<Row> selectAll(String tableName, List<SqlExpression> where, Connection connection) throws SQLException {
		return select(tableName, null, where, connection);
	}
	
	public static List<Row> selectAll(String tableName, String where, Connection connection) throws SQLException {
		return selectAll(tableName, parseValuesListLiteral(where), connection);
	}
	
	private static List<SqlExpression> parseValuesListLiteral(String value) {
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
	}
	
	private static String buildWhere(List<SqlExpression> where) {
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
	}
	
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
	
	public static List<Row> executeSelect(SqlSelect sqlSelect, Connection connection) throws SQLException {
		if(sqlSelect.getFields() != null) {
			return select(sqlSelect.getTableName(), sqlSelect.getFields(), sqlSelect.getWhereExpression(), connection);
		}
		return selectAll(sqlSelect.getTableName(), sqlSelect.getWhereExpression(), connection);
	}
	
	public static int executeInsert(SqlInsert sqlInsert, Connection connection) throws SQLException {
		return insertIntoTable(sqlInsert.getTableName(), sqlInsert.getRowToInsert().get(0), connection);
	}
	
	public int executeInsertMultiRows(SqlInsert sqlInsert) throws SQLException {
		return insertIntoTableMultiRows(sqlInsert.getTableName(), sqlInsert.getRowToInsert());
	}
	
	public static int executeUpdate(SqlUpdate sqlUpdate, Connection connection) throws SQLException {
		return updateTable(sqlUpdate.getTableName(), sqlUpdate.getUpdatesData(), sqlUpdate.getWhereExpression(), connection);
	}
	
	public static List<SqlExpression> parseWhere(JSONArray whereData) throws JSONException {
		List<SqlExpression> result = new ArrayList<>();
		//JSONArray whereData = new JSONArray(jsonArray);
		for (int i = 0; i < whereData.length(); i++) {
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
	}
	
	public static List<SqlExpression> parseCellDataRow(String jsonCallDataArray) throws JSONException {
		return parseCellDataRow(new JSONArray(jsonCallDataArray));
	}
	
	public static List<List<SqlExpression>> parseCellDataRows(String jsonCallDataArray) throws JSONException {
		return parseCellDataRows(new JSONArray(jsonCallDataArray));
	}
	
	public static List<SqlExpression> parseCellDataRow(JSONArray rowArray) throws JSONException {
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
	}
	
	public static List<List<SqlExpression>> parseCellDataRows(JSONArray rowsArray) throws JSONException {
		List<List<SqlExpression>> result = new ArrayList<>();
		for (int i = 0; i < rowsArray.length(); i++) {
			result.add(parseCellDataRow(rowsArray.getJSONArray(i)));
		}
		return result;
	}
	
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
}
