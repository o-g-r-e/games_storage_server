package com.my.gamesdataserver;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.sql.SQLException;

import com.my.gamesdataserver.basedbclasses.DataBaseInterface;
import com.my.gamesdataserver.dbengineclasses.GamesDbEngine;
import com.my.gamesdataserver.helpers.EmailSender;
import com.my.gamesdataserver.helpers.LogManager;
import com.my.gamesdataserver.helpers.Settings;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Main {
	
	private static DataBaseInterface dbInterface;
	private static Settings settings;
	private static LogManager logManager;
	private static SslContext sslCtx;
	
	public static void main(String[] args) {
		EventLoopGroup bossGroup = null;
		EventLoopGroup workerGroup = null;
		
		try {
			
			String settingsPath = null;
			if(args.length < 1) {
				settingsPath = new File(".").getCanonicalPath()+File.separator+"settings"+File.separator+".settings";
			} else {
				settingsPath = args[0];
			}
			
			logManager = new LogManager(8);
			
			settings = new Settings(new File(settingsPath));
			
			dbInterface = new DataBaseInterface(new DataBaseConnectionParameters("jdbc:mysql", settings.get("dbAddr"), 
																							   settings.get("dbPort"), 
																							   settings.get("dbName"), 
																							   settings.get("dbUser"), 
																							   settings.get("dbPassword")));
			
			bossGroup = new NioEventLoopGroup();
	        workerGroup = new NioEventLoopGroup();
	        
	        EmailSender.enabled = "Yes".equals(settings.get("sendEmail"));
	        
	        boolean sslEnable = "Yes".equals(settings.get("enableSsl"));
	        
	        if(sslEnable) {
	        	File tlsCert = new File(settings.get("cert"));
	        	File tlsPrivateKey = new File(settings.get("privateKey"));
	        	sslCtx = SslContextBuilder.forServer(tlsCert, tlsPrivateKey).sslProvider(SslProvider.OPENSSL).clientAuth(ClientAuth.NONE).build();
	        }
	        ServerBootstrap b = new ServerBootstrap();
	        b.group(bossGroup, workerGroup)
	        	.channel(NioServerSocketChannel.class)
	        	.childHandler(new ChannelInitializer<SocketChannel>() {
	        		@Override
	                public void initChannel(SocketChannel ch) throws Exception {
	                	ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
	                	if(sslEnable) ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
	                    ch.pipeline().addLast(new ClientHandler(dbInterface, logManager));
	                }
	        	});
	            
	        b.bind(Integer.parseInt(settings.get("serverPort"))).sync().channel().closeFuture().sync();
	        
		} catch (SQLException | IOException /*| CertificateException*/ | InterruptedException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logManager.log("initialization_error", "", sw.toString(), "");
		} finally {
            if(workerGroup != null) workerGroup.shutdownGracefully();
            if(bossGroup != null) bossGroup.shutdownGracefully();
        }
	}
}
