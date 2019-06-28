package com.my.gamesdataserver;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	private static DataBaseManager dbManager;
	
	public static void main(String[] args) throws IOException {
		
		if(args.length < 1) {
			System.out.println("Port is not set.");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		try {
			dbManager = new DataBaseManager(new DataBaseConnectionParameters("jdbc:mysql", "localhost", "3306", "games_data", "root", "1234567890"));
		
			ServerSocket serverSocket = new ServerSocket(port);
			ExecutorService executorService = Executors.newFixedThreadPool(8);
			System.out.println("Server started.");
		    try {
		    	while (true) {
		    		Socket socket = serverSocket.accept(); //wait for new connection
		            executorService.submit(new ClientProcessor(socket, dbManager));
		    	}
		    } finally {
		    	serverSocket.close();
		    	dbManager.closeConnection();
		    	System.out.println("Server closed.");
		    }
	      
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
}
