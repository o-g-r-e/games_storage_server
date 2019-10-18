package com.my.gamesdataserver;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.sql.SQLException;

import com.my.gamesdataserver.gamesdbmanager.GamesDbManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Main {
	
	private static GamesDbManager dbManager;
	private static Settings settings;
	private static LogManager logManager;
	
	public static void main(String[] args) {
		EventLoopGroup bossGroup = null;
		EventLoopGroup workerGroup = null;
		
		try {
			
			String settingsPath = "";
			if(args.length < 1) {
				settingsPath = new File(".").getCanonicalPath()+File.separator+"settings"+File.separator+".settings";
			} else {
				settingsPath = args[0];
			}
			
			logManager = new LogManager(8);
			
			settings = new Settings(new File(settingsPath));
			
			dbManager = new GamesDbManager(new DataBaseConnectionParameters("jdbc:mysql", settings.getParameter("dbAddr"), 
																						  settings.getParameter("dbPort"), 
																						  settings.getParameter("dbName"), 
																						  settings.getParameter("dbUser"), 
																						  settings.getParameter("dbPassword")));
			
			bossGroup = new NioEventLoopGroup();
	        workerGroup = new NioEventLoopGroup();
	        SelfSignedCertificate ssc = new SelfSignedCertificate();
	        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
	        
	        ServerBootstrap b = new ServerBootstrap();
	        b.group(bossGroup, workerGroup)
	        	.channel(NioServerSocketChannel.class)
	        	.childHandler(new ChannelInitializer<SocketChannel>() {
	        		@Override
	                public void initChannel(SocketChannel ch) throws Exception {
	                	ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
	                	//ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
	                    ch.pipeline().addLast(new ClientHandler(dbManager, logManager, settings));
	                }
	        	});
	            
	        b.bind(Integer.parseInt(settings.getParameter("serverPort"))).sync().channel().closeFuture().sync();
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
	      
		} catch (SQLException | IOException | CertificateException | NumberFormatException | InterruptedException e) {
			logManager.log("system", e.getMessage());
		} finally {
            if(workerGroup != null) workerGroup.shutdownGracefully();
            if(bossGroup != null) bossGroup.shutdownGracefully();
        }
	}
}
