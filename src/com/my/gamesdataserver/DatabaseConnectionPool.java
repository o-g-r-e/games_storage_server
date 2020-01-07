package com.my.gamesdataserver;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DatabaseConnectionPool implements DatabaseConnectionManager {
	//private MysqlDataSource mysqlDataSource;
	private ComboPooledDataSource cpds;
	
	//private boolean connectionReleased = true;
	//private Connection currentConnection;
	
	public DatabaseConnectionPool(DataBaseConnectionParameters dbConnectionParameters) throws SQLException, PropertyVetoException {
		
			cpds = new ComboPooledDataSource();
			cpds.setDriverClass("com.mysql.jdbc.Driver");
		    cpds.setJdbcUrl(/*"jdbc:mysql://localhost:3306/dbName"*/dbConnectionParameters.toString()); 
		    cpds.setUser(dbConnectionParameters.getUser());                                   
		    cpds.setPassword(dbConnectionParameters.getPassword());

		    Properties properties = new Properties();
		    properties.setProperty("user", dbConnectionParameters.getUser());
		    properties.setProperty("password", dbConnectionParameters.getPassword());
		    properties.setProperty("useUnicode", "true");
		    properties.setProperty("characterEncoding", "UTF8");
		    cpds.setProperties(properties);
			
		    // set options
		    cpds.setMaxStatements(180);
		    cpds.setMaxStatementsPerConnection(180);
		    cpds.setMinPoolSize(50);
		    cpds.setAcquireIncrement(10);
		    cpds.setMaxPoolSize(60);
		    cpds.setMaxIdleTime(30);
		
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

	@Override
	public Connection getConnection() throws SQLException {
		/*if(currentConnection == null || connectionReleased) {
			currentConnection = cpds.getConnection();
			connectionReleased = false;
		}
		return currentConnection;*/
		return cpds.getConnection();
	}

	/*@Override
	public void releaseConnection() throws SQLException {
		currentConnection.close();
		currentConnection = null;
		connectionReleased = true;
	}

	public boolean isConnectionReleased() {
		return connectionReleased;
	}*/
}
