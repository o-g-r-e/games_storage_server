package com.my.gamesdataserver;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionManager {
	Connection getConnection() throws SQLException;
	//void releaseConnection() throws SQLException;
}
