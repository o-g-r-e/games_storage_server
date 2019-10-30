package com.my.gamesdataserver.basedbclasses;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DataBaseInterface {
	private Connection driverManagerConnection;
	private MysqlDataSource mysqlDataSource;
	private boolean printQuery = true;
	private boolean transaction = false;
	private DataBaseConnectionParameters dbConnectionParameters;

	public DataBaseInterface(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		this.dbConnectionParameters = dbConnectionParameters;
		driverManagerConnection = DriverManager.getConnection(dbConnectionParameters.getUrl(), dbConnectionParameters.getUser(), dbConnectionParameters.getPassword());
		
		/*mysqlDataSource = new MysqlDataSource();
		mysqlDataSource.setUser(dbConnectionParameters.getUser());
		mysqlDataSource.setPassword(dbConnectionParameters.getPassword());
		mysqlDataSource.setDatabaseName("games_data");
		mysqlDataSource.setUseSSL(false);*/
		//d.setUrl("jdbc:mysql://localhost:3306/games_data");
		
		/*ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc://localhost", null);
	    PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
	    ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
	    poolableConnectionFactory.setPool(connectionPool);
	    PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(connectionPool);*/
	}

	protected Connection getCon() throws SQLException {
		return driverManagerConnection;
		//return mysqlDataSource.getConnection();
	}
	
	public void enableTransactions() throws SQLException {
		getCon().setAutoCommit(false);
		transaction = true;
	}
	
	public void disableTransactions() throws SQLException {
		getCon().setAutoCommit(true);
		transaction = false;
	}
	
	public void commit() throws SQLException {
		getCon().commit();
	}
	
	public void rollback() throws SQLException {
		getCon().rollback();
	}
	
	public boolean isTransactionsEnabled() throws SQLException {
		//return getCon().getAutoCommit();
		return transaction;
	}
	
	public void deleteFrom(String tableName, String where) throws SQLException {
		
		if(where.contains("&")) {
			where = where.replaceAll("&", " AND ");
		}
		
		getCon().prepareStatement("DELETE FROM `"+tableName+"` WHERE ("+where+")").execute();
	}
	
	public void createTable(String name, ColData[] fields) throws SQLException {
		StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS "+name+" (`id` INT NOT NULL AUTO_INCREMENT, ");
		
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
			
			if(fields[i].getDefaultValue() != null && !"".equals(fields[i].getDefaultValue().trim())) {
				query.append(" DEFAULT ").append(fields[i].getDefaultValue());
			}
			
			if(i < fields.length-1) {
				query.append(", ");
			}
		}
		
		query.append(", PRIMARY KEY (`id`));");
		
		getCon().prepareStatement(query.toString()).execute();
	}
	
	public void dropTable(String tableName) throws SQLException {
		getCon().prepareStatement("DROP TABLE `"+tableName+"`").execute();
	}
	
	public int insertIntoTable(String tableName, List<CellData> row) throws SQLException {
		StringBuilder fieldsNames = new StringBuilder("(");
		StringBuilder fieldsValues = new StringBuilder("(");
		int width = row.size();
		for (int i = 0; i < width; i++) {
			CellData d = row.get(i);
			fieldsNames.append(d.getName());
			fieldsValues.append("?");
			if(i < width-1) {
				fieldsNames.append(",");
				fieldsValues.append(",");
			}
		}
		
		fieldsNames.append(")");
		fieldsValues.append(")");
		
		/*StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
		sqlInsert.append(tableName);
		sqlInsert.append(" ");
		sqlInsert.append(fieldsNames);
		sqlInsert.append(" VALUES ");
		sqlInsert.append(fieldsValues);
		for (int i = 1; i < values.size(); i++) {
			sqlInsert.append(",");
			sqlInsert.append(fieldsValues);
		}*/
		
		PreparedStatement pstmt = getCon().prepareStatement(String.format("INSERT INTO %s %s VALUES %s", tableName, fieldsNames, fieldsValues));
		
		for (int i = 0; i < row.size(); i++) {
			setQueryValue(pstmt, row.get(i), i+1);
		}
		
		return pstmt.executeUpdate();
	}
	
	public int insertIntoTableMultiRows(String tableName, List<List<CellData>> row) throws SQLException {
		return 0;
	}
	
	public int updateTable(String tableName, List<CellData> set, List<CellData> where) throws SQLException {
		StringBuilder sqlUpdate = new StringBuilder("UPDATE ");
		sqlUpdate.append(tableName);
		sqlUpdate.append(" SET ");
		for (int i = 0; i < set.size(); i++) {
			CellData d = set.get(i);
			sqlUpdate.append(d.getName());
			sqlUpdate.append("=?");
			if(i < set.size()-1) {
				sqlUpdate.append(",");
			}
		}
		sqlUpdate.append(" WHERE (");
		for (int i = 0; i < where.size(); i++) {
			CellData d = where.get(i);
			sqlUpdate.append(d.getName());
			sqlUpdate.append("=?");
			if(i < where.size()-1) {
				sqlUpdate.append(" AND ");
			}
		}
		sqlUpdate.append(")");
		
		PreparedStatement pstmt = getCon().prepareStatement(sqlUpdate.toString());//con.prepareStatement(String.format("UDPATE %s SET %s WHERE %s", tableName, ));
		
		for (int i = 0; i < set.size(); i++) {
			setQueryValue(pstmt, set.get(i), i+1);
		}
		
		for (int i = 0; i < where.size(); i++) {
			setQueryValue(pstmt, where.get(i), i+1+set.size());
		}
		
		return pstmt.executeUpdate();
	}
	
	public List<List<CellData>> selectAll(String tableName) throws SQLException {
		String sql = String.format("SELECT * FROM %s", tableName);
		PreparedStatement pstmt = getCon().prepareStatement(sql);
		List<List<CellData>> result = new ArrayList<>();
		ResultSet resultSet = pstmt.executeQuery();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		while(resultSet.next()) {
			List<CellData> row = new ArrayList<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				row.add(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(row);
		}
		return result;
	}
	
	public List<List<CellData>> selectAllWhere(String tableName, List<CellData> where) throws SQLException {
		
		StringBuilder sql = new StringBuilder("SELECT * FROM "+tableName);
		
		if(where.size() > 0) {
			
			sql.append(" WHERE ");
			
			for (int i = 0; i < where.size(); i++) {
				CellData d = where.get(i);
				sql.append(d.getName());
				sql.append("=?");
				if(i < where.size()-1) {
					sql.append(" AND ");
				}
			}
		}
		
		PreparedStatement pstmt = getCon().prepareStatement(sql.toString());
		for (int i = 0; i < where.size(); i++) {
			setQueryValue(pstmt, where.get(i), i+1);
		}
		ResultSet resultSet = pstmt.executeQuery();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		List<List<CellData>> result = new ArrayList<>();
		while(resultSet.next()) {
			List<CellData> row = new ArrayList<>();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				row.add(new CellData(rsmd.getColumnType(i), rsmd.getColumnName(i), resultSet.getObject(i)));
			}
			result.add(row);
		}
		return result;
	}
	
	public List<List<CellData>> selectAllWhere(String tableName, String where) throws SQLException {
		
		List<CellData> whereData = new ArrayList<>();
		Map<String, String> wherePairs = new HashMap<>();
		List<List<CellData>> result = new ArrayList<>();
		
		String[] pairs = where.split("&");
		
		if(pairs.length >= 2 || where.contains("=")) {
			for(String pair : pairs) {
				String[] params = pair.split("=");
				
				if(params.length < 2) {
					continue;
				}
				wherePairs.put(params[0], params[1]);
			}
		}
		
		for(Map.Entry<String, String> e : wherePairs.entrySet()) {
			whereData.add(new CellData(0, e.getKey(), e.getValue()));
		}
		
		result = selectAllWhere(tableName, whereData);
		
		return result;
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
	
	private void setQueryValue(PreparedStatement pstmt, CellData d, int index) throws SQLException {
		switch (d.getType()) {
		case Types.INTEGER:
			pstmt.setInt(index, (int)d.getValue());
			break;
		case Types.VARCHAR:
			pstmt.setString(index, String.valueOf(d.getValue()));
			break;
		case Types.FLOAT:
			pstmt.setFloat(index, (float)d.getValue());
			break;
		default:
			pstmt.setObject(index, d.getValue());
			break;
		}
	}
	
	public String[] findTablesByPrefix(String tableNamePrefix) throws SQLException {
		PreparedStatement pstmt = getCon().prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '?' AND TABLE_NAME LIKE '?%'");
		pstmt.setString(1, dbConnectionParameters.getDataBaseName());
		pstmt.setString(2, tableNamePrefix);
		ResultSet resultSet = pstmt.executeQuery();
		List<String> tableNames = new ArrayList<>();
		while(resultSet.next()) {
			tableNames.add(resultSet.getString(1));
		}
		return tableNames.toArray(new String[] {});
	}
	
	public List<List<CellData>> executeSelect(SqlSelect sqlSelect) throws SQLException {
		return selectAllWhere(sqlSelect.getTableName(), sqlSelect.getWhereExpression());
	}
	
	public int executeInsert(SqlInsert sqlInsert) throws SQLException {
		return insertIntoTable(sqlInsert.getTableName(), sqlInsert.getRowToInsert().get(0));
	}
	
	public int executeInsertMultiRows(SqlInsert sqlInsert) throws SQLException {
		return insertIntoTableMultiRows(sqlInsert.getTableName(), sqlInsert.getRowToInsert());
	}
	
	public int executeUpdate(SqlUpdate sqlUpdate) throws SQLException {
		return updateTable(sqlUpdate.getTableName(), sqlUpdate.getUpdatesData(), sqlUpdate.getWhereExpression());
	}
	
	public static List<CellData> parseCellDataRow(String jsonCallDataArray) throws JSONException {
		return parseCellDataRow(new JSONArray(jsonCallDataArray));
	}
	
	public static List<List<CellData>> parseCellDataRows(String jsonCallDataArray) throws JSONException {
		return parseCellDataRows(new JSONArray(jsonCallDataArray));
	}
	
	public static List<CellData> parseCellDataRow(JSONArray rowArray) throws JSONException {
		List<CellData> result = new ArrayList<>();
		for (int i = 0; i < rowArray.length(); i++) {
			JSONObject cellObject = rowArray.getJSONObject(i);
			if(!cellObject.has("name") || !cellObject.has("value")) {
				continue;
			}
			if(cellObject.has("type")) {
				result.add(new CellData(DataBaseInterface.parseDataType(cellObject.getString("type")), cellObject.getString("name"), cellObject.getString("value")));
			} else {
				result.add(new CellData(cellObject.getString("name"), cellObject.getString("value")));
			}
		}
		return result;
	}
	
	public static List<List<CellData>> parseCellDataRows(JSONArray rowsArray) throws JSONException {
		List<List<CellData>> result = new ArrayList<>();
		for (int i = 0; i < rowsArray.length(); i++) {
			result.add(parseCellDataRow(rowsArray.getJSONArray(i)));
		}
		return result;
	}
	
	public void createIndex(String indexName, String tableName, String[] fields, boolean unique) throws SQLException {
		getCon().prepareStatement(String.format("CREATE %s INDEX %s ON %s(%s)", unique?"UNIQUE":"", indexName, tableName, String.join(",", fields))).execute();
	}
}
