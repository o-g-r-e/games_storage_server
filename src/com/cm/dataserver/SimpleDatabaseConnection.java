package com.cm.dataserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDatabaseConnection implements DatabaseConnectionManager {
	private Connection driverManagerConnection;
	
	public SimpleDatabaseConnection(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		driverManagerConnection = DriverManager.getConnection(dbConnectionParameters.toString(), dbConnectionParameters.getUser(), dbConnectionParameters.getPassword());
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return driverManagerConnection;
	}
}
