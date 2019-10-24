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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.ColData;
import com.my.gamesdataserver.basedbclasses.DataBaseInterface;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlRequest;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.dbengineclasses.ApiKey;
import com.my.gamesdataserver.dbengineclasses.DatabaseEngine;
import com.my.gamesdataserver.dbengineclasses.Game;
import com.my.gamesdataserver.defaultgameclasses.BaseGameDbInterface;
import com.my.gamesdataserver.defaultgameclasses.Level;
import com.my.gamesdataserver.defaultgameclasses.Player;
import com.my.gamesdataserver.defaultgameclasses.PlayerData;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	
	private DataBaseInterface dbInterface;
	private DatabaseEngine dbManager;
	private LogManager logManager;
	private Access access = new Access();
	private StringBuilder errorLogMessage = new StringBuilder();
	private HttpRequest inputHttpRequest = new HttpRequest();
	private HttpResponse httpResponse = new HttpResponse("1.1", 200, "");
	private String errorLogFilePrefix = "error";
	private static final GameTemplate match3Template = createMatch3Template();
	
	private enum RequestGroup {BASE, API};
	
	public ClientHandler(DataBaseInterface dbInterface, LogManager logManager) throws IOException {
		this.dbInterface = dbInterface;
		this.dbManager = new DatabaseEngine(dbInterface);
		this.logManager = logManager;
	}
	
	private RequestGroup recognizeRequestGroup(HttpRequest httpRequest) {
		if(httpRequest.getUrl().startsWith("/api")) {
			return RequestGroup.API;
		} else if(httpRequest.getUrl().startsWith("/system")) {
			return RequestGroup.BASE;
		} else {
			return null;
		}
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		String inputString = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
	    ReferenceCountUtil.release(msg);
	    
	    inputHttpRequest.parse(inputString, true);
	    
		if(access.isDiscardRequest(inputHttpRequest.getUrl()) || access.isWrongSymbols(inputHttpRequest.getUrl()))  {
			logManager.log("access", inputString+"\n\n"+"Suppressed because not accessible");
			return;
		}
		
		RequestGroup requestGroup = recognizeRequestGroup(inputHttpRequest);
		
		try {
			
			switch (requestGroup) {

			case BASE:
				handleSystemRequest(ctx, inputHttpRequest);
				break;
				
			case API:
				handleApiRequest(ctx, inputHttpRequest);
				break;
				
			default:
				httpResponse.setContent(simpleJsonObject("Error", "Bad request group"));
				sendHttpResponse(ctx, httpResponse);
			}
			
			if(errorLogMessage.length() > 0) {
				logManager.log(errorLogFilePrefix, inputString+"\n\n"+errorLogMessage+"\n\n"+getHttpResponseLog(httpResponse, false));
			}
		
		} catch (JSONException | SQLException | MessagingException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			try {
				if(dbInterface.isTransactionsEnabled()) {
					dbInterface.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace(pw);
			}
			
			e.printStackTrace(pw);
			logManager.log(errorLogFilePrefix, inputString+"\n\n"+errorLogMessage+"\n\n"+sw.toString()+"\n\n"+getHttpResponseLog(httpResponse, false));
			httpResponse.setContent(simpleJsonObject("Error", "An error occurred during processing"));
			sendHttpResponse(ctx, httpResponse);
		}
    }
	
	private void handleSystemRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException, MessagingException {
		if(httpRequest.getUrl().startsWith("/system/generate_api_key")) {
			
			DatabaseEngine dbManager = new DatabaseEngine(dbInterface);
			if(!simpleValidation(new String[] {"email"}, httpRequest.getUrlParametrs())) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String inputEmail = httpRequest.getUrlParametrs().get("email");
			
			if(!dbManager.checkOwnerByEmail(inputEmail)) {
				int result = dbManager.regOwner(inputEmail);
				if(result <= 0) {
					errorLogMessage.append("registration fail");
				}
			}
			
			String newApiKey = RandomKeyGenerator.nextString(45);
			int added = dbManager.writeNewApiKey(inputEmail, newApiKey);
			
			if(added > 0) {
				EmailSender.send(inputEmail, "New API key generation", "Your API key generated: "+newApiKey);
				httpResponse.setContent(simpleJsonObject("Success", "API key generated successfully"));
			} else {
				errorLogMessage.append("Cannot write new api key");
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while creating the key"));
			}
			sendHttpResponse(ctx, httpResponse);
		} else if(httpRequest.getUrl().startsWith("/system/register_game")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters(true);
			
			if(!simpleValidation(new String[] {"api_key", "game_name", "game_package", "game_type"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			String gameName = contentParameters.get("game_name");
			String gameJavaPackage = contentParameters.get("game_package");
			String gameType = contentParameters.get("game_type");
			
			if(dbManager.checkGameByKey(apiKey)) {
				httpResponse.setContent(simpleJsonObject("Error", "Game alredy exists"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			int ownerId = dbManager.getApiKey(apiKey).getOwnerId();
			
			if(ownerId < 1) {
				errorLogMessage.append("Cannot find game api key");
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while searching for the key"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbInterface.enableTransactions();
			String prefix = DatabaseEngine.generateTablePrefix(gameName, apiKey);
			int added = dbManager.insertGame(gameName, gameJavaPackage, ownerId, apiKey, gameType, prefix);
			
			if(added < 1) {
				dbInterface.rollback();
				errorLogMessage.append("Cannot write to games");
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while adding the game"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbManager.removeApiKey(apiKey);
			
			dbInterface.commit();
			dbInterface.disableTransactions();
			
			switch (gameType) {
			case "match3":
				dbManager.createGameTables(match3Template, prefix); //throw exception if not successfully
				break;
			}
			
			EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game "+apiKey+" registered");
			httpResponse.setContent(simpleJsonObject("Success", "Register successed"));
			sendHttpResponse(ctx, httpResponse);
			
		} else if(httpRequest.getUrl().startsWith("/system/udpate_game_data")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters(true);
			
			if(!simpleValidation(new String[] {"api_key"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			int updated = dbManager.updateGame(contentParameters.get("new_game_name"), contentParameters.get("new_game_package"));
			// update tables names ???
			if(updated > 0) {
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game data "+contentParameters.get("api_key")+" updated");
			}
			
		} else if(httpRequest.getUrl().startsWith("/system/delete_game")) {
			if(!simpleValidation(new String[] {"api_key"}, httpRequest.getUrlParametrs())) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = httpRequest.getUrlParametrs().get("api_key");
			
			Game game = dbManager.deleteGame(apiKey);
			if(game == null) {
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while deleting the game"));
				return;
			}
			
			dbManager.deleteGameTables(game.getPrefix(), match3Template.getTableNames());
			
			httpResponse.setContent(simpleJsonObject("Success", "Deletion was successful"));
			sendHttpResponse(ctx, httpResponse);
		} else {
			httpResponse.setContent(simpleJsonObject("Error", "Bad command"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
	}
	
	private String parseTableName(HttpRequest httpRequest) {
		return httpRequest.getUrlParametrs().get("table");
	}
	
	private SqlRequest parseRequest(HttpRequest httpRequest, String tableNamePrefix) throws JSONException {
		SqlRequest result = null;
		String tableName = tableNamePrefix+httpRequest.getUrlParametrs().get("table");
		
		if(httpRequest.getUrl().startsWith("/api/select")) {
			String json = httpRequest.getContent();
			if(httpRequest.getContent().length() <= 0) {
				json = "[{\"type\":\"STRING\",\"name\":\"playerId\",\"value\":\""+httpRequest.getUrlParametrs().get("player_id")+"\"}]";
			}
			result = new SqlSelect(tableName, json);
		} else if(httpRequest.getUrl().startsWith("/api/insert")) {
			String json = httpRequest.getContent();
			result = new SqlInsert(tableName, json, json);
		} else if(httpRequest.getUrl().startsWith("/api/update")) {
			String jsonUpdateData = httpRequest.getContent();
			JSONObject updateData = new JSONObject(jsonUpdateData);
			JSONArray jsonWhereData = updateData.getJSONArray("where");
			JSONArray jsonSetData = updateData.getJSONArray("set");
			result = new SqlUpdate(tableName, jsonWhereData.toString(), jsonSetData.toString());
		}
			
		return result;
		
	}
	
	private String rowsToJson(List<List<CellData>> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < rows.size(); i++) {
			List<CellData> row = rows.get(i);
			sb.append("{");
			for (int j = 0; j < row.size(); j++) {
				CellData cellData = row.get(j);
				sb.append("\"").append(cellData.getName()).append("\":");
				if(cellData.getType() == Types.VARCHAR) {
					sb.append("\"").append(cellData.getValue()).append("\"");
				} else {
					sb.append(cellData.getValue());
				}
				if(j < row.size()-1) {
					sb.append(",");
				}
			}
			sb.append("}");
			if(i < rows.size()-1) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	private void handleApiRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException, JSONException {
		
		if(!simpleValidation(new String[] {"player_id", "api_key", "table"}, httpRequest.getUrlParametrs())) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String apiKey = httpRequest.getUrlParametrs().get("api_key");
		
		Game game = dbManager.getGameByKey(apiKey);
		
		if(game == null) {
			httpResponse.setContent(simpleJsonObject("Error", "Game not found"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
		
		SqlRequest sqlRequest = parseRequest(httpRequest, game.getPrefix());
		
		if(sqlRequest instanceof SqlSelect) {
			List<List<CellData>> rows = dbInterface.executeSelect((SqlSelect) sqlRequest);
			httpResponse.setContent(rowsToJson(rows));
		} else if(sqlRequest instanceof SqlInsert) {
			int result = dbInterface.executeInsert((SqlInsert) sqlRequest);
			if(result > 0) {
				httpResponse.setContent(simpleJsonObject("Success", "Insert completed successfully"));
			} else {
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while inserting"));
			}
		} else if(sqlRequest instanceof SqlUpdate) {
			int result = dbInterface.executeUpdate((SqlUpdate) sqlRequest);
			if(result > 0) {
				httpResponse.setContent(simpleJsonObject("Success", "Update completed successfully"));
			} else {
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while updating"));
			}
		}
		
		sendHttpResponse(ctx, httpResponse);
		
		/*if(httpRequest.getUrl().startsWith("/api/read_all")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"player_id", "api_key"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			
			Game game = dbManager.getGameByKey(apiKey);
			
			if(game == null) {
				httpResponse.setContent(simpleJsonObject("Error", "Game not found"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String playerId = contentParameters.get("player_id");
			String gameType = game.getType();
			
			switch (gameType) {
			case "match3":
				
				match3dbManager.setTablePrefix(game.getPrefix());
				
				PlayerData playerData = match3dbManager.readPlayerData(playerId);
				
				if(playerData == null) {
					httpResponse.setContent(simpleJsonObject("Error", "Data not found"));
				} else {
					httpResponse.setContent(playerData.toJson());
				}
				
				sendHttpResponse(ctx, httpResponse);
				break;
			}
		} else if(httpRequest.getUrl().startsWith("/api/level_complete")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"player_id", "api_key", "level", "stars", "scores"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			
			Game game = dbManager.getGameByKey(apiKey);
			
			if(game == null) {
				httpResponse.setContent(simpleJsonObject("Error", "Game not found"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String playerId = contentParameters.get("player_id");
			int level = Integer.parseInt(contentParameters.get("level"));
			int stars = Integer.parseInt(contentParameters.get("stars"));
			int scores = Integer.parseInt(contentParameters.get("scores"));
			String gameType = game.getType();
			
			switch (gameType) {
			case "match3":
				
				match3dbManager.setTablePrefix(game.getPrefix());
				
				List<Level> levels = match3dbManager.getLevelsOfPlayer(playerId);
				int rowsActions = 0;
				
				boolean isNewLevel = true;
				
				for(Level lvl : levels) {
					if(lvl.getLevel() == level) {
						isNewLevel = false;
						break;
					}
				}
				
				if(isNewLevel) {
					rowsActions = match3dbManager.addLevel(playerId, level, scores, stars);
				} else {
					rowsActions = match3dbManager.updateLevel(playerId, level, scores, stars);
				}
				
				if(rowsActions > 0) {
					httpResponse.setContent(simpleJsonObject("Success", "Levels data updated"));
				} else {
					httpResponse.setContent(simpleJsonObject("Error", "An error occurred at runtime. Level has not been updated."));
				}
				
				sendHttpResponse(ctx, httpResponse);
				break;
			}
			
		} else if(httpRequest.getUrl().startsWith("/api/add_boost")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"player_id", "api_key", "boost_name"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String playerId = contentParameters.get("player_id");
			String apiKey = contentParameters.get("api_key");
			String boostName = contentParameters.get("boost_name");*/
			
			/*if(dbManager.getPlayer(playerId, apiKey) == null) {
				dbManager.addPlayer(playerId, apiKey);
			}*/
			
		/*} else if(httpRequest.getUrl().startsWith("/api/spend_boost")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"player_id", "api_key", "boost_name"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String playerId = contentParameters.get("player_id");
			String apiKey = contentParameters.get("api_key");
			String boostName = contentParameters.get("boost_name");*/
			
			/*if(dbManager.getPlayer(playerId, apiKey) == null) {
				dbManager.addPlayer(playerId, apiKey);
			}*/
			
		/*}*/
	}
	
	private static GameTemplate createMatch3Template() {
		
		List<TableTemplate> tt = new ArrayList<>();
		tt.add(new TableTemplate("scorelevel", new ColData[] {new ColData(Types.INTEGER, "playerId"),
															  new ColData(Types.INTEGER, "level"),
															  new ColData(Types.INTEGER, "score"),
															  new ColData(Types.INTEGER, "stars")}));
		
		tt.add(new TableTemplate("players", new ColData[] {new ColData(Types.VARCHAR, "playerId"), new ColData(Types.INTEGER, "max_level")}));
		
		tt.add(new TableTemplate("boosts", new ColData[] {new ColData(Types.INTEGER, "playerId"), 
														  new ColData(Types.VARCHAR, "name"), 
														  new ColData(Types.INTEGER, "count")}));
		
		return new GameTemplate(GameTemplate.Types.MATCH3, tt);
	}
	
	private boolean simpleValidation(String[] names, Map<String, String> parameters) {
		for(String name : names) {
			if(!parameters.containsKey(name) || "".equals(parameters.get(name))) {
				return false;
			}
		}
		return true;
	}
	
	private boolean validateApi(String[] names, Map<String, String> parameters, String apiKeyVarName) throws SQLException {
		if(!simpleValidation(new String[] {"player_id", "api_key"}, parameters)) {
			return false;
		}
		
		if(apiKeyVarName == null || parameters.get(apiKeyVarName) == null  || dbManager.getGameByKey(parameters.get(apiKeyVarName)) == null) {
			return false;
		}
		
		return true;
	}
	
	private void sendValidationFailResponse(ChannelHandlerContext ctx) {
		httpResponse.setContent(simpleJsonObject("Error", "Parameters validation failed"));
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
		httpResponse.addHeader("Access-Control-Allow-Origin", "*");
		httpResponse.addHeader("Content-type", "application/json");
		/*httpResponse.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
		httpResponse.addHeader("Access-Control-Max-Age", "1000");
		httpResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");*/
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
