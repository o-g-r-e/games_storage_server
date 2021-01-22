package com.cm.dataserver;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionManager {
	Connection getConnection() throws SQLException;
	//void releaseConnection() throws SQLException;
}
