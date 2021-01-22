package com.cm.dataserver.basedbclasses;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.DataBaseConnectionParameters;
import com.cm.dataserver.DatabaseConnectionManager;
import com.cm.dataserver.DatabaseConnectionPoolC3P0;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.mchange.v2.c3p0.ComboPooledDataSource;

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
	
	public static List<Row> select(String sql, Connection connection) throws SQLException {
		Statement pstmt = connection.createStatement();
		ResultSet resultSet = pstmt.executeQuery(sql);
		ResultSetMetaData rsmd = resultSet.getMetaData();
		List<Row> result = new ArrayList<>();
		
		while(resultSet.next()) {
			Row row = new Row();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				row.addCell(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(row);
		}
		return result;
	}
	
	public static List<Row> select(String sql, List<QueryTypedValue> queryValues, Connection connection) throws SQLException {
		PreparedStatement pstmt = connection.prepareStatement(sql);
		
		setQueryValues(pstmt, queryValues);
		
		ResultSet resultSet = pstmt.executeQuery();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		List<Row> result = new ArrayList<>();
		
		while(resultSet.next()) {
			Row row = new Row();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				row.addCell(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(row);
		}
		return result;
	}
	
	public static List<Row> select(String sql, QueryTypedValue queryValue, Connection connection) throws SQLException {
		List<QueryTypedValue> queryValues = new ArrayList<>();
		queryValues.add(queryValue);
		return select(sql, queryValues, connection);
	}
	
	public static int update(String sql, Connection connection) throws SQLException {
		return connection.createStatement().executeUpdate(sql);
	}
	
	public static int update(String sql, List<QueryTypedValue> queryValues, Connection connection) throws SQLException {
		PreparedStatement pstmt = connection.prepareStatement(sql);
		
		setQueryValues(pstmt, queryValues);
		
		return pstmt.executeUpdate();
	}
	
	public static int update(String sql, QueryTypedValue queryValue, Connection connection) throws SQLException {
		List<QueryTypedValue> queryValues = new ArrayList<>();
		queryValues.add(queryValue);
		return update(sql, queryValues, connection);
	}
	
	public static int insert(String sql, Connection connection) throws SQLException {
		return update(sql, connection);
	}
	
	public static int insert(String sql, List<QueryTypedValue> queryValues, Connection connection) throws SQLException {
		return update(sql, queryValues, connection);
	}
	
	public static int insert(String sql, QueryTypedValue queryValue, Connection connection) throws SQLException {
		return update(sql, queryValue, connection);
	}
	
	/*public static int parseDataType(String type) {
		switch (type) {
		case "INTEGER":
			return Types.INTEGER;
		case "STRING":
			return Types.VARCHAR;
		case "FLOAT":
			return Types.FLOAT;
		}
		return -1;
	}*/
	
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
	
	public static void setQueryValues(PreparedStatement pstmt, List<QueryTypedValue> values) throws SQLException {
		for (int i = 0; i < values.size(); i++) {
			setQueryValue(pstmt, values.get(i).getType(), values.get(i).getValue(), i+1);
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
	
	public static void createIndex(String indexName, String tableName, String[] fields, boolean unique, Connection connection) throws SQLException {
		connection.prepareStatement(String.format("CREATE %s INDEX %s ON %s(%s)", unique?"UNIQUE":"", indexName, tableName, String.join(",", fields))).execute();
	}
}
