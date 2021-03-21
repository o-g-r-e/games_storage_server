package com.cm.dataserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.json.JSONException;

import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.GameTemplate;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.handlers.GameHandler;
import com.cm.dataserver.handlers.HttpResponseTemplates;
import com.cm.dataserver.handlers.PlayerHandler;
import com.cm.dataserver.handlers.RootHandler;
import com.cm.dataserver.handlers.SystemHandler;
import com.cm.dataserver.helpers.EmailSender;
import com.cm.dataserver.helpers.LogManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class ClientHandlerMoreOOP extends SimpleChannelInboundHandler<FullHttpRequest> {
	private DatabaseConnectionManager dbConnectionPool;
	private Connection dbConnection;
	private LogManager logManager;
	private static final GameTemplate MATCH_3_TEMPLATE = GameTemplate.match3Template();
	private final String defaultPlayerIdFieldName = "playerId";
	private EmailSender emailSender;
	private static Map<Class<?>, RootHandler> handlerClasses;
	private static Map<String, Method> handlers;
	//private Game currentGame;
	
	public ClientHandlerMoreOOP(DatabaseConnectionManager dbConnectionPool, LogManager logManager, EmailSender emailSender) throws IOException {
		this.dbConnectionPool = dbConnectionPool;
		this.logManager = logManager;
		this.emailSender = emailSender;
		
		handlerClasses = new HashMap<>();
		//handlerClasses.put(SystemHandler.class, new SystemHandler(dbConnection, MATCH_3_TEMPLATE, emailSender));
		//handlerClasses.put(PlayerHandler.class, new PlayerHandler(dbConnection));
		handlerClasses.put(GameHandler.class, new GameHandler(dbConnection/*, currentGame*/));
		
		handlers = new HashMap<>();
		for(Map.Entry<Class<?>, RootHandler> handlerClass : handlerClasses.entrySet()) {
			for (Method m : handlerClass.getValue().getClass().getDeclaredMethods()) {
				if (m.isAnnotationPresent(UriAnnotation.class)) {
					UriAnnotation uriAnn = m.getAnnotation(UriAnnotation.class);
					handlers.put(uriAnn.uri(), m);
				}
			}
		}
	}
	
	private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
		ctx.writeAndFlush(httpResponse);
	}
	
	private void sendGameNotFound(ChannelHandlerContext ctx) {
		sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
		//ServerURI.RequestGroup requestGroup = ServerURI.requestGroup(fullHttpRequest.uri());
		try {
			dbConnection = dbConnectionPool.getConnection();
			
			//currentGame = parseGame(fullHttpRequest);
			
			if("/system/register_game".equals(fullHttpRequest.uri())) {
				
				new SystemHandler(dbConnection, MATCH_3_TEMPLATE, emailSender).handleRegisterGame(ctx, fullHttpRequest);
				
			} else if("/player/authorization".equals(fullHttpRequest.uri())) {
				
				Game game = Authorization.parseGame(fullHttpRequest, dbConnection);
				if(game == null) {
					sendGameNotFound(ctx);
					return;
				}
				
				new PlayerHandler(dbConnection).handlePlayerRequest(ctx, fullHttpRequest, game);
				
			} else {
				Authorization auth = new Authorization();
				
				if(!auth.checkAuthorizationHeader(fullHttpRequest)) {
					sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
					return;
				}
				
				Game game = Authorization.parseGame(fullHttpRequest, dbConnection);
				if(game == null) {
					sendGameNotFound(ctx);
					return;
				}
				
				PlayerId playerId = new PlayerId("playerId", fullHttpRequest.headers().get(Authorization.PLAYER_ID_HEADER));
				
				if(!auth.checkPlayerId(playerId.getValue(), dbConnection)) {
					sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
					return;
				}
				
				String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
				
				Method handler = handlers.get(fullHttpRequest.uri());
				handler.invoke(handlerClasses.get(handler.getClass()), ctx, inputContent, game, playerId);
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			try {
				if(dbConnection != null && !dbConnection.getAutoCommit()) {
					dbConnection.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace(pw);
			}
			
			e.printStackTrace(pw);
			FullHttpResponse httpResponse = HttpResponseTemplates.buildSimpleResponse("Error", e.getMessage()/*"An error occurred during processing"*/, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logManager.error(LogManager.httpRequestToString(fullHttpRequest), sw.toString(), httpResponse.toString());
			//sendHttpResponse(ctx, httpResponse);
		} finally {
			try {
				if(dbConnection != null) dbConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
