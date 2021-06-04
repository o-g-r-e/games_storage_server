package com.cm.dataserver;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import com.cm.dataserver.helpers.LogManager;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class ServerSslProvider {
	private SslContext sslContext;
	private static ServerSslProvider instance;
	private ScheduledFuture<?> reloadSslScheduledFuture;
	private ScheduledExecutorService scheduler;
	private LogManager logManager;
	
	private ServerSslProvider() {
		scheduler = Executors.newScheduledThreadPool(1);
	}
	
	public ServerSslProvider init(File cert, File privateKey) throws SSLException {
		sslContext = SslContextBuilder.forServer(cert, privateKey).sslProvider(SslProvider.OPENSSL).clientAuth(ClientAuth.NONE).build();
		
		if(reloadSslScheduledFuture != null && !reloadSslScheduledFuture.isDone()) reloadSslScheduledFuture.cancel(false);
		
		reloadSslScheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				String message = "";
				try {
					File tlsCert = cert;
		        	File tlsPrivateKey = privateKey;
					sslContext = SslContextBuilder.forServer(tlsCert, tlsPrivateKey).sslProvider(SslProvider.OPENSSL).clientAuth(ClientAuth.NONE).build();
					message = "Sertificate reloaded\n"
							+ tlsCert.toString()+"\n"
							+ tlsPrivateKey.toString()+"\n";
				} catch (Exception e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					message = sw.toString();
				}
				
				logManager.log("ssl_renew", message);
			}}, 0, 30, TimeUnit.DAYS);
		return this;
	}
	
	public static ServerSslProvider getInstance() throws SSLException {
		if(instance == null) return new ServerSslProvider();
		return instance;
	}
	
	public SslContext getSslContext() {
		return sslContext;
	}
	
	public void setLogManager(LogManager logManager) {
		this.logManager = logManager;
	}
}
