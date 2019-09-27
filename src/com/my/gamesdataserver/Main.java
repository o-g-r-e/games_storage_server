package com.my.gamesdataserver;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Main {
	
	private static DataBaseManager dbManager;
	private static Settings settings;
	
	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException, CertificateException {
		
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
	        SelfSignedCertificate ssc = new SelfSignedCertificate();
	        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
	        try {
	            ServerBootstrap b = new ServerBootstrap();
	            b.group(bossGroup, workerGroup)
	             .channel(NioServerSocketChannel.class)
	             .childHandler(new ChannelInitializer<SocketChannel>() {
	                 @Override
	                 public void initChannel(SocketChannel ch) throws Exception {
	                	 ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
	                	 //ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
	                     ch.pipeline().addLast(new ClientHandler(dbManager));
	                 }
	             });
	            
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
