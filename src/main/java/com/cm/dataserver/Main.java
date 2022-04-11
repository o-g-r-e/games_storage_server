package com.cm.dataserver;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;

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
			settings = new Settings(new File(p("./settings/.settings")));
			
			DatabaseConnectionManager dbcm = new DatabaseConnectionPoolApache(new DataBaseConnectionParameters("jdbc:mysql", settings.getString("dbAddr"), 
																												   settings.getString("dbPort"), 
																												   settings.getString("dbName"), 
																												   settings.getString("dbUser"), 
																												   settings.getString("dbPassword")));
			
			emailSender = new EmailSender(settings.getString("smtpServer"), settings.getString("smtpUser"), settings.getString("oauthClientId"), settings.getString("oauthClientSecret"), settings.getString("oauthRefreshToken"), settings.getString("oauthTokenUrl"), settings.getString("accessTokenPath"), settings.getString("emailFrom"), logManager);
	        emailSender.enable(settings.getBool("sendEmail"));
			
			bossGroup = new NioEventLoopGroup();
	        workerGroup = new NioEventLoopGroup();
	        
	        boolean sslEnable = settings.getBool("enableSsl");
	        ServerSslProvider serverSslProvider = ServerSslProvider.getInstance();
	        if(sslEnable) {
	        	File tlsCert = new File(settings.getString("cert"));
	        	File tlsPrivateKey = new File(settings.getString("privateKey"));
	        	//sslContext = SslContextBuilder.forServer(tlsCert, tlsPrivateKey).sslProvider(SslProvider.OPENSSL).clientAuth(ClientAuth.NONE).build();
	        	serverSslProvider.init(tlsCert, tlsPrivateKey).setLogManager(logManager);
	        }
	        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().allowedRequestHeaders("Authorization", "api_key", "player_id", "Test-Object-Query", "Content-Type").build();
	        ServerBootstrap b = new ServerBootstrap();
	        b.group(bossGroup, workerGroup)
	        	.channel(NioServerSocketChannel.class)
	        	.childHandler(new ChannelInitializer<SocketChannel>() {
	        		@Override
	                public void initChannel(SocketChannel channel) throws Exception {
	        			ChannelPipeline pipeline = channel.pipeline();
	        			if(sslEnable) pipeline.addLast(serverSslProvider.getSslContext().newHandler(channel.alloc()));
	        			pipeline.addLast(new HttpRequestDecoder());
	        			pipeline.addLast(new HttpResponseEncoder());
	        			pipeline.addLast(new HttpObjectAggregator(1048576));
	                	//channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(2048));
	        			pipeline.addLast(new CorsHandler(corsConfig));
	                	pipeline.addLast(new ClientHandlerMoreOOP(dbcm, logManager, emailSender, settings));
	                }
	        	});
	            
	        b.bind(Integer.parseInt(settings.getString("serverPort"))).sync().channel().closeFuture().sync();
	        
		} catch (IOException /*| CertificateException*/ | InterruptedException e) {
			/* StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw); */
			e.printStackTrace();
		} finally {
            if(workerGroup != null) workerGroup.shutdownGracefully();
            if(bossGroup != null) bossGroup.shutdownGracefully();
        }
	}

	private static String p(String path) {
		return path.replaceAll("/", Matcher.quoteReplacement(File.separator));
	}
}
