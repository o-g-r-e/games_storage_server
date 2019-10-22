package com.my.gamesdataserver.rawdbclasses;
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

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.Parse;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DataBaseInterface {
	private Connection driverManagerConnection;
	private MysqlDataSource mysqlDataSource;
	private boolean printQuery = true;
	private boolean transaction = false;

	public DataBaseInterface(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		
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
				query.append("INT NULL");
				break;
			case Types.VARCHAR:
				query.append("VARCHAR(45) NULL");
				break;
			case Types.FLOAT:
				query.append("FLOAT NULL");
				break;
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
		StringBuilder sqlWhere = new StringBuilder();
		for (int i = 0; i < where.size(); i++) {
			CellData d = where.get(i);
			sqlWhere.append(d.getName());
			sqlWhere.append("=?");
			if(i < where.size()-1) {
				sqlWhere.append(" AND ");
			}
		}
		
		String sql = String.format("SELECT * FROM %s WHERE %s", tableName, sqlWhere);
		PreparedStatement pstmt = getCon().prepareStatement(sql);
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
		Map<String, String> p = new HashMap<>();
		List<List<CellData>> result = new ArrayList<>();
		
		if(where.contains("&")) {
			String[] arr = where.split("&");
			for(String s : arr) {
				p.put(s.split("=")[0], s.split("=")[1]);
			}
		} else if (where.contains("=")) {
			p = Parse.spltBy(where, "=");
		} else {
			return result;
		}
		
		/*Map<String, String> p = new HashMap<>();
		String[] s1 = where.split("&");
		for(String s : s1) {
			String[] s2 = s.split("=");
			p.put(s2[0], s2[1]);
		}*/
		
		for(Map.Entry<String, String> e : p.entrySet()) {
			whereData.add(new CellData(0, e.getKey(), e.getValue()));
		}
		
		result = selectAllWhere(tableName, whereData);
		
		return result;
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
}
