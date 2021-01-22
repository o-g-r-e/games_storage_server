package com.cm.dataserver;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.cm.dataserver.helpers.EmailSender;
import com.cm.dataserver.helpers.LogManager;
import com.cm.dataserver.helpers.Settings;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class Main {
	
	private static Settings settings;
	private static SslContext sslContext;
	private static LogManager logManager;
	private static EmailSender emailSender;
	
	public static void main(String[] args) {
		
		EventLoopGroup bossGroup = null;
		EventLoopGroup workerGroup = null;
		
		try {
			logManager = new LogManager(8);
			String settingsPath = null;
			if(args.length < 1) {
				settingsPath = new File(".").getCanonicalPath()+File.separator+"settings"+File.separator+".settings";
			} else {
				settingsPath = args[0];
			}
			
			
			settings = new Settings(new File(settingsPath));
			
			DatabaseConnectionManager dbcm = new DatabaseConnectionPoolApache(new DataBaseConnectionParameters("jdbc:mysql", settings.get("dbAddr"), 
																												   settings.get("dbPort"), 
																												   settings.get("dbName"), 
																												   settings.get("dbUser"), 
																												   settings.get("dbPassword")));
			
			emailSender = new EmailSender(settings.get("smtpServer"), settings.get("smtpUser"), settings.get("oauthClientId"), settings.get("oauthClientSecret"), settings.get("oauthRefreshToken"), settings.get("oauthTokenUrl"), settings.get("accessTokenPath"), settings.get("emailFrom"), logManager);
	        emailSender.enable("Yes".equals(settings.get("sendEmail")));
			
			bossGroup = new NioEventLoopGroup();
	        workerGroup = new NioEventLoopGroup();
	        
	        boolean sslEnable = "Yes".equals(settings.get("enableSsl"));
	        
	        if(sslEnable) {
	        	File tlsCert = new File(settings.get("cert"));
	        	File tlsPrivateKey = new File(settings.get("privateKey"));
	        	sslContext = SslContextBuilder.forServer(tlsCert, tlsPrivateKey).sslProvider(SslProvider.OPENSSL).clientAuth(ClientAuth.NONE).build();
	        }
	        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().allowedRequestHeaders("Authorization", "api_key", "player_id", "Test-Object-Query").build();
	        ServerBootstrap b = new ServerBootstrap();
	        b.group(bossGroup, workerGroup)
	        	.channel(NioServerSocketChannel.class)
	        	.childHandler(new ChannelInitializer<SocketChannel>() {
	        		@Override
	                public void initChannel(SocketChannel channel) throws Exception {
	        			ChannelPipeline pipeline = channel.pipeline();
	        			if(sslEnable) pipeline.addLast(sslContext.newHandler(channel.alloc()));
	        			pipeline.addLast(new HttpRequestDecoder());
	        			pipeline.addLast(new HttpResponseEncoder());
	        			pipeline.addLast(new HttpObjectAggregator(1048576));
	                	//channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
	        			pipeline.addLast(new CorsHandler(corsConfig));
	                	pipeline.addLast(new ClientHandler(dbcm, logManager, emailSender));
	                }
	        	});
	            
	        b.bind(Integer.parseInt(settings.get("serverPort"))).sync().channel().closeFuture().sync();
	        
		} catch (IOException /*| CertificateException*/ | InterruptedException e) {
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
