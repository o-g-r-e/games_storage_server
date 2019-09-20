package com.my.gamesdataserver;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Main {
	
	private static DataBaseManager dbManager;
	private static Settings settings;
	
	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException {
		settings = new Settings();
		
		if(args.length < 1) {
			String workPath = new File(".").getAbsolutePath();
			settings.readSettings(new File(workPath+File.separator+Settings.defaultSettingsFilePath));
		} else {
			settings.readSettings(new File(args[0]));
		}
		
		try {
			dbManager = new DataBaseManager(new DataBaseConnectionParameters("jdbc:mysql", settings.getDbAddr(), 
																						   settings.getDbPort(), 
																						   settings.getDbName(), 
																						   settings.getDbUser(), 
																						   settings.getDbPassword()));
			
			EventLoopGroup bossGroup = new NioEventLoopGroup();
	        EventLoopGroup workerGroup = new NioEventLoopGroup();
	        try {
	            ServerBootstrap b = new ServerBootstrap();
	            b.group(bossGroup, workerGroup)
	             .channel(NioServerSocketChannel.class)
	             .childHandler(new ChannelInitializer<SocketChannel>() {
	                 @Override
	                 public void initChannel(SocketChannel ch) throws Exception {
	                     ch.pipeline().addLast(new ClientHandler(dbManager));
	                 }
	             })
	             .option(ChannelOption.SO_BACKLOG, 128)
	             .childOption(ChannelOption.SO_KEEPALIVE, true);
	            
	            b.bind(Integer.parseInt(settings.getServerPort())).sync().channel().closeFuture().sync();
	            System.out.println("Server started.");
	        } finally {
	            workerGroup.shutdownGracefully();
	            bossGroup.shutdownGracefully();
	        }
		
			/*ServerSocket serverSocket = new ServerSocket(Integer.parseInt(settings.getServerPort()));
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
		    }*/
	      
		} catch (SQLException e1) {
			e1.printStackTrace();
			//System.err.println("Could not create connection to database server.");
		}
	}
}
