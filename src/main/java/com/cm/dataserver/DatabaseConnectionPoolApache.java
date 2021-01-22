package com.cm.dataserver;

import java.sql.Connection;
import java.sql.SQLException;

//import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;

public class DatabaseConnectionPoolApache implements DatabaseConnectionManager {
	private BasicDataSource dataSource;
	
	public DatabaseConnectionPoolApache(DataBaseConnectionParameters dbConnectionParameters) {
		dataSource = new BasicDataSource();
		dataSource.setUrl(dbConnectionParameters.toString());
		dataSource.setUsername(dbConnectionParameters.getUser());
		dataSource.setPassword(dbConnectionParameters.getPassword());
		
		dataSource.setMinIdle(5);
		dataSource.setMaxIdle(10);
		dataSource.setMaxOpenPreparedStatements(100);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
}
