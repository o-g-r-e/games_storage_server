package com.my.gamesdataserver;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.json.JSONException;

import com.my.gamesdataserver.apisqlrequest.AbstractSqlRequest;
import com.my.gamesdataserver.apisqlrequest.SqlInsert;
import com.my.gamesdataserver.apisqlrequest.SqlSelect;
import com.my.gamesdataserver.apisqlrequest.SqlUpdate;
import com.my.gamesdataserver.gamesdbmanager.GameEntity;
import com.my.gamesdataserver.gamesdbmanager.GamesDbManager;
import com.my.gamesdataserver.rawdbmanager.CellData;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	
	private GamesDbManager dbm;
	private Access access;
	private LogManager logManager;
	private StringBuilder errorLogMessage = new StringBuilder();
	private HttpRequest inputHttpRequest = new HttpRequest();
	private HttpResponse httpResponse = new HttpResponse("1.1", 200, "");
	private String logPrefix = "error";
	private static final String[] MATH_3_TABLE_SET = {"ScoreLevel", "Level", "Boosts"};
	
	private enum RequestGroup {API, SYSTEM, CONTENT, BAD};
	
	public ClientHandler(GamesDbManager dbm, LogManager logManager) throws IOException {
		this.dbm = dbm;
		this.access = new Access();
		this.logManager = logManager;
	}
	
	private RequestGroup recognizeRequestGroup(HttpRequest httpRequest) {
		if(httpRequest.getUrl().startsWith("/api")) {
			return RequestGroup.API;
		} else if(httpRequest.getUrl().startsWith("/content")) {
			return RequestGroup.CONTENT;
		} else if(httpRequest.getUrl().startsWith("/system")) {
			return RequestGroup.SYSTEM;
		} else {
			return RequestGroup.BAD;
		}
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		String inputString;
	    
	    inputString = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
	    ReferenceCountUtil.release(msg);
	    
	    inputHttpRequest.parse(inputString);
	    
		if(access.isDiscardRequest(inputHttpRequest.getUrl()) || access.isWrongSymbols(inputHttpRequest.getUrl()))  {
			logManager.log("access", inputString+"\n\n"+"Suppressed because not accessible");
			return;
		}
		
		RequestGroup requestGroup = recognizeRequestGroup(inputHttpRequest);
		
		try {
			
			switch (requestGroup) {
			case API:
				handleApiRequest(ctx, inputHttpRequest);
				break;

			case SYSTEM:
				handleSystemRequest(ctx, inputHttpRequest);
				break;
				
			case CONTENT:
				break;
				
			default:
				httpResponse.setContent(simpleJsonObject("Internal error", "Bad request group"));
				sendHttpResponse(ctx, httpResponse);
			}
			
			if(errorLogMessage.length() > 0) {
				logManager.log(logPrefix, inputString+"\n\n"+errorLogMessage+"\n\n"+getHttpResponseLog(httpResponse, false));
			}
		
		} catch (JSONException | SQLException | MessagingException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			try {
				if(dbm.isTransactionsEnabled()) {
					dbm.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace(pw);
			}
			
			e.printStackTrace(pw);
			logManager.log(logPrefix, inputString+"\n\n"+errorLogMessage+"\n\n"+sw.toString()+"\n\n"+getHttpResponseLog(httpResponse, false));
			httpResponse.setContent("Internal error");
			sendHttpResponse(ctx, httpResponse);
		}
    }
	
	private void handleApiRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws JSONException, SQLException {
		AbstractSqlRequest.Type command = AbstractSqlRequest.parseCommand(httpRequest.getUrl());
		
		if(command == null) {
			httpResponse.setContent(simpleJsonObject("Internal error", "Cannot recognize command"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
		
		AbstractSqlRequest request = initRequest(command, httpRequest);
			
		if(request == null) {
			httpResponse.setContent(simpleJsonObject("Internal error", "Cannot create request"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
			
		executeApiRequest(request, ctx);
			
		/*if(request.validate()) {
			commandRequestProcessor(command, request, ctx);
		} else if(access.isPermittedPath(httpRequest.getUrl()) && httpRequest.getUrlParametrs().containsKey("key") && httpRequest.getUrlParametrs().get("key").equals(Access.contentAccessKey)) {
			contentRequestProcessor(httpRequest.getUrl(), ctx);
		} else {
			sendHttpResponse(ctx, simpleJsonObject("Request error", "Request failed validation"));
			return;
		}*/
	}
	
	private void handleSystemRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException, MessagingException {
		SystemRequestsType systemRequestsType = null;
		
		/*if(httpRequest.getUrl().startsWith("/register")) {
			systemRequestsType = SystemRequestsType.NEW_OWNER;
			
			Map<String, String> m = httpRequest.parseContentWithParameters();
			String activationKey = RandomKeyGenerator.nextString(16);
			int result = dbm.preRegOwner(m.get("new_owner_name"), activationKey);
			
			if(result <= 0) {
				httpResponse.setContent(simpleJsonObject("Internal error", "Registration error"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String registerLink = settings.getParameter("serverAddress")+":"+settings.getParameter("serverPort")+"/activate?key="+activationKey;
			httpResponse.setContent(registerLink);
			sendHttpResponse(ctx, httpResponse);
		
		} else if(httpRequest.getUrl().startsWith("/activate")) {
			systemRequestsType = SystemRequestsType.REGISTER_OWNER;
			
			String activationKey = httpRequest.getUrlParametrs().get("key");
			
			List<DataCell> where = new ArrayList<>();
			where.add(new DataCell(Types.VARCHAR, "activation_id", activationKey));
			
			List<List<DataCell>> result = dbm.selectWhere("pre_reg_owners", where);
			
			if(result.size() <= 0) {
				httpResponse.setContent(simpleJsonObject("Error", "Reg key not found"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			int ownerId = dbm.regOwner((String)result.get(0).get(1).getValue());
			if(ownerId <= 0) {
				httpResponse.setContent("Registration error");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			httpResponse.setContent(ownerId+"");
			sendHttpResponse(ctx, httpResponse);
		} else*/if(httpRequest.getUrl().startsWith("/system/generate_api_key")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!contentParameters.containsKey("email")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String inputEmail = contentParameters.get("email");
			
			if(!dbm.checkOwnerByEmail(inputEmail)) {
				int result = dbm.regOwner(inputEmail);
				if(result <= 0) {
					errorLogMessage.append("registration fail");
				}
			}
			
			String newApiKey = RandomKeyGenerator.nextString(45);
			int added = dbm.writeNewApiKey(inputEmail, newApiKey);
			
			if(added > 0) {
				EmailSender.send(contentParameters.get("email"), "New API key generation", "Your API key generated: "+newApiKey);
				httpResponse.setContent("API key generated successfully");
				sendHttpResponse(ctx, httpResponse);
			} else {
				errorLogMessage.append("Cannot write new api key");
				httpResponse.setContent("Internal error");
				sendHttpResponse(ctx, httpResponse);
			}
		} else if(httpRequest.getUrl().startsWith("/system/register_game")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			if(!contentParameters.containsKey("api_key") || 
					!contentParameters.containsKey("game_name") ||
						!contentParameters.containsKey("game_package") ||
							!contentParameters.containsKey("game_type")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			String gameName = contentParameters.get("game_name");
			String gameJavaPackage = contentParameters.get("game_package");
			String gameType = contentParameters.get("game_type");
			
			if(dbm.checkGameByKey(apiKey)) {
				httpResponse.setContent("Game alredy exists");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			int ownerId = dbm.readOwnerIdFromApiKeys(apiKey);
				
			if(ownerId < 1) {
				errorLogMessage.append("Cannot find game api key");
				httpResponse.setContent("Internal error");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
				
			dbm.enableTransactions();
			String prefix = gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 4)+"_";
			int added = dbm.insertGame(gameName, gameJavaPackage, ownerId, apiKey, MATH_3_TABLE_SET, prefix);
				
			if(added < 1) {
				dbm.rollback();
				errorLogMessage.append("Cannot write to games");
				httpResponse.setContent("Internal error");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbm.removeApiKey(apiKey);
			
			dbm.commit();
			dbm.disableTransactions();
			
			switch (gameType) {
			case "match3":
				dbm.createMatch3TableSet(prefix); //throw exception if not successfully
				break;
			}
			
			EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game "+apiKey+" registered");
			httpResponse.setContent("Register success");
			sendHttpResponse(ctx, httpResponse);
			
		} else if(httpRequest.getUrl().startsWith("/system/udpate_game_data")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!contentParameters.containsKey("api_key")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			int updated = dbm.updateGame(contentParameters.get("new_game_name"), contentParameters.get("new_game_package"));
			if(updated > 0) {
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game data "+contentParameters.get("api_key")+" updated");
			}
			
		} else if(httpRequest.getUrl().startsWith("/system/delete_game")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!contentParameters.containsKey("api_key")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			if(dbm.deleteGameByApiKey(contentParameters.get("api_key"))) {
				httpResponse.setContent("Game deleted");
			} else {
				httpResponse.setContent("Game delete failed");
			}
			sendHttpResponse(ctx, httpResponse);
		} /*else if(httpRequest.getUrl().startsWith("/add_game")) {
			systemRequestsType = SystemRequestsType.ADD_GAME;
			
			Map<String, String> parsedContent = httpRequest.parseContentWithParameters();
			
			if(!parsedContent.containsKey("game_name") || !parsedContent.containsKey("owner_id") || !dbm.checkOwner(parsedContent.get("owner_id"))) {
				httpResponse.setContent("Some errors found in request");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String gameKey = RandomKeyGenerator.nextString(45);
			int added = dbm.insertGame(Integer.parseInt(parsedContent.get("owner_id")), parsedContent.get("game_name"), gameKey);
			
			if(added <= 0) {
				httpResponse.setContent("Some errors found in request");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			httpResponse.setContent(gameKey);
			sendHttpResponse(ctx, httpResponse);
		}*/ /*else if(httpRequest.getUrl().startsWith("/gen_key")) {
			String activationKey = RandomKeyGenerator.nextString(16);
			httpResponse.setContent(activationKey);
			sendHttpResponse(ctx, httpResponse);
		}*/ /*else if(httpRequest.getUrl().startsWith("/add_player")) {
			systemRequestsType = SystemRequestsType.ADD_PLAYER;
			
			Map<String, String> urlParametrs = httpRequest.getUrlParametrs();
			if(!urlParametrs.containsKey("player_id") || !dbm.checkPlayer(urlParametrs.get("player_id"))) {
				httpResponse.setContent("Some errors found in request");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
		}*/ else {
			httpResponse.setContent(simpleJsonObject("Internal error", "Cannot recognize command"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
		
		
	}
	
	private AbstractSqlRequest initRequest(AbstractSqlRequest.Type command, HttpRequest httpRequest) throws JSONException {
		switch (command) {
		case INSERT_INTO_TABLE:
			return new SqlInsert(httpRequest);
			
		case UPDATE_TABLE:
			return new SqlUpdate(httpRequest);
			
		case SELECT:
			return new SqlSelect(httpRequest);
		}
		
		return null;
	}
	
	void executeApiRequest(AbstractSqlRequest request, ChannelHandlerContext ctx) throws SQLException {
		
		if(!request.validate()) {
			httpResponse.setContent(simpleJsonObject("Request error", "Request failed validation"));
		} else if(request instanceof SqlInsert) {
			
			//int changed = dbm.insertIntoTable(request.getTableName(), ((SqlInsert)request).getData());
			//httpResponse.setContent(simpleJsonObject("Row's added", ""+changed));
			
		} else if (request instanceof SqlUpdate) {
			
			int changed = dbm.updateTable(request.getTableName(), ((SqlUpdate)request).getSet(), ((SqlUpdate)request).getWhere());
			httpResponse.setContent(simpleJsonObject("Row's updated", ""+changed));
			
		} else if(request instanceof SqlSelect) {
			List<List<CellData>> result = dbm.selectAll(request.getTableName());
			httpResponse.setContent(simpleJsonObject("Result", ""+result.size()));
		}
		
		sendHttpResponse(ctx, httpResponse);
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
	
	String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
    
	void sendHttpResponse(ChannelHandlerContext ctx, HttpResponse httpResponse) {
		ctx.writeAndFlush(Unpooled.copiedBuffer(httpResponse.toString(), CharsetUtil.UTF_8));
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
	
	private String getHttpResponseLog(HttpResponse httpResponse , boolean cutContent) {
		StringBuilder result = new StringBuilder();
		result.append("\nHTTP response to: ").append("\n");
		result.append(httpResponse.getHeader());
		if(httpResponse.getContent() != null && httpResponse.getContent().length() > 0) {
			if(cutContent) {
				result.append(httpResponse.getContentCharacters(255));
			} else {
				result.append(httpResponse.getContent());
			}
		}
		return result.toString();
	}
}
