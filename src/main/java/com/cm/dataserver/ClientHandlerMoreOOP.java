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

import com.cm.databaseserver.exceptions.AuthorizationException;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.GameTemplate;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.handlers.ApiHandler;
import com.cm.dataserver.handlers.GameHandler;
import com.cm.dataserver.handlers.MessagesHandler;
import com.cm.dataserver.handlers.PlayerHandler;
import com.cm.dataserver.handlers.RootHandler;
import com.cm.dataserver.handlers.SystemHandler;
import com.cm.dataserver.helpers.EmailSender;
import com.cm.dataserver.helpers.HttpResponseTemplates;
import com.cm.dataserver.helpers.LogManager;
import com.cm.dataserver.helpers.Settings;

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
	private final String defaultPlayerIdFieldName = "playerId";
	private EmailSender emailSender;
	private static Map<Class<?>, RootHandler> handlerClasses = new HashMap<>();
	private static Map<String, Method> handlers;
	private Settings settings;
	
	static {
		//handlerClasses.put(SystemHandler.class, new SystemHandler(dbConnection, MATCH_3_TEMPLATE, emailSender));
		//handlerClasses.put(PlayerHandler.class, new PlayerHandler(dbConnection));
		handlerClasses.put(GameHandler.class, new GameHandler());
		handlerClasses.put(ApiHandler.class, new ApiHandler());
		handlerClasses.put(MessagesHandler.class, new MessagesHandler());
		
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
	
	public ClientHandlerMoreOOP(DatabaseConnectionManager dbConnectionPool, LogManager logManager, EmailSender emailSender, Settings settings) throws IOException {
		this.dbConnectionPool = dbConnectionPool;
		this.logManager = logManager;
		this.emailSender = emailSender;
		this.settings = settings;
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
				
				new SystemHandler(dbConnection, emailSender, settings.getBool("allowTestInvoice")).handleRegisterGame(ctx, fullHttpRequest);
				
			} else if("/player/authorization".equals(fullHttpRequest.uri())) {
				Game game = authorization(ctx, fullHttpRequest);
				
				new PlayerHandler(dbConnection).handlePlayerRequest(ctx, fullHttpRequest, game);
				
			} else if("/echo".equals(fullHttpRequest.uri())) {
				sendHttpResponse(ctx, HttpResponseTemplates.response("{\"status\":\"success\",\"message\":\"Echo OK\"}", HttpResponseStatus.OK));
			} else {
				Game game = authorization(ctx, fullHttpRequest);
				
				statistic(game, fullHttpRequest.uri(), dbConnection);
				
				PlayerId playerId = new PlayerId("playerId", fullHttpRequest.headers().get(Authorization.PLAYER_ID_HEADER));
				
				if(!StringDataHelper.validatePlayerId(playerId.getValue())) {
					sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "Player authorization fail", HttpResponseStatus.OK));
					return;
				}
				
				String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
				
				Method handler = handlers.get(fullHttpRequest.uri());
				handler.invoke(handlerClasses.get(handler.getDeclaringClass()), ctx, inputContent, game, playerId, dbConnection);
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
			sendHttpResponse(ctx, httpResponse);
		} finally {
			try {
				if(dbConnection != null) dbConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Game authorization(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws InvalidKeyException, NoSuchAlgorithmException, AuthorizationException, SQLException {
		Authorization.AuthValue auth = Authorization.authorization(fullHttpRequest);
		
		Game game = DataBaseMethods.getGameByHash(auth.getGameHash(), dbConnection);
		
		if(game == null) {
			throw new AuthorizationException("Authorization Exception: Game not found");
		}
		
		return game;
	}
	
	private void statistic(Game game, String uri, Connection dbConnection) throws SQLException {
		GameHandler.updateStatistic(game, uri, dbConnection);
	}
}
