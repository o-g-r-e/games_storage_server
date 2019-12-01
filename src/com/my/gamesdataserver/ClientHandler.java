package com.my.gamesdataserver;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import javax.print.attribute.standard.MediaSize.Engineering;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.SqlExpression;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.ColData;
import com.my.gamesdataserver.basedbclasses.DataBaseInterface;
import com.my.gamesdataserver.basedbclasses.Decrement;
import com.my.gamesdataserver.basedbclasses.Increment;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlRequest;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.dbengineclasses.OwnerSecrets;
import com.my.gamesdataserver.dbengineclasses.GamesDbEngine;
import com.my.gamesdataserver.dbengineclasses.Owner;
import com.my.gamesdataserver.dbengineclasses.TableIndex;
import com.my.gamesdataserver.dbengineclasses.TableTemplate;
import com.my.gamesdataserver.helpers.EmailSender;
import com.my.gamesdataserver.helpers.LogManager;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;
import com.my.gamesdataserver.template1classes.Template1DbEngine;
import com.my.gamesdataserver.dbengineclasses.Game;
import com.my.gamesdataserver.dbengineclasses.GameTemplate;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

public class ClientHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private GamesDbEngine dbManager;
	private Template1DbEngine template1DbManager;
	private LogManager logManager;
	private String errorLogFilePrefix = "error";
	private static final GameTemplate TEMPLATE_1;
	private static Map<String, String> defaultResponseHeaders;
	private boolean hmac;
	
	private enum RequestGroup {BASE, API, TEMPLATE_API, BAD};
	
	static {
		TEMPLATE_1 = createGameTemplate1();
		defaultResponseHeaders = new HashMap<>();
		defaultResponseHeaders.put("Access-Control-Allow-Origin", "*");
		defaultResponseHeaders.put("Content-type", "application/json");
		/*defaultResponseHeaders.put("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
		defaultResponseHeaders.put("Access-Control-Max-Age", "1000");
		defaultResponseHeaders.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");*/
	}
	
	public ClientHandler(DataBaseInterface dbInterface, LogManager logManager, boolean isHmac) throws IOException {
		this.dbManager = new GamesDbEngine(dbInterface);
		this.template1DbManager = new Template1DbEngine(dbInterface);
		this.logManager = logManager;
		this.hmac = isHmac;
	}
	
	private RequestGroup recognizeRequestGroup(FullHttpRequest httpRequest) {
		if(httpRequest.uri().startsWith("/api")) {
			return RequestGroup.API;
		} else if(httpRequest.uri().startsWith("/api/template1")) {
			return RequestGroup.TEMPLATE_API;
		} else if(httpRequest.uri().startsWith("/system")) {
			return RequestGroup.BASE;
		} else {
			return RequestGroup.BAD;
		}
	}
	
	@Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		RequestGroup requestGroup = recognizeRequestGroup(fullHttpRequest);
		
		try {
			
			switch (requestGroup) {

			case BASE:
				handleSystemRequest(ctx, fullHttpRequest);
				break;
				
			case API:
				handleApiRequest(ctx, fullHttpRequest);
				break;
				
			case TEMPLATE_API:
				handleTemplateRequest(ctx, fullHttpRequest);
				break;
				
			case BAD:
				FullHttpResponse httpResponse = buildSimpleResponse("Error", "Bad request group", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders);
				logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), "ERROR: Bad request group".toUpperCase(), httpResponse.toString());
				sendHttpResponse(ctx, httpResponse);
				break;
			}
		} catch (JSONException | SQLException | MessagingException | InvalidKeyException | NoSuchAlgorithmException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			try {
				if(dbManager.isTransactionsEnabled()) {
					dbManager.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace(pw);
			}
			
			e.printStackTrace(pw);
			FullHttpResponse httpResponse = buildSimpleResponse("Error", "An error occurred during processing", HttpResponseStatus.INTERNAL_SERVER_ERROR, defaultResponseHeaders);
			logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), sw.toString(), httpResponse.toString());
			sendHttpResponse(ctx, httpResponse);
		}
    }
	
	private void handleTemplateRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException {
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key", "table", "playerId"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String apiKey = urlParameters.get("api_key");
		
		Game game = dbManager.getGameByKey(apiKey);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
			return;
		}
		
		template1DbManager.setTablePrefix(game.getPrefix());
		
		String playerId = urlParameters.get("playerId");
		
		Player player = template1DbManager.getPlayer(playerId);
		
		if(player == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Player not found", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
			return;
		}
		
		if(httpRequest.uri().startsWith("/api/template1/completeLevel")) {
			
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK, defaultResponseHeaders));
	}

	private void handleSystemRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, MessagingException, InvalidKeyException, NoSuchAlgorithmException {
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(httpRequest.uri().startsWith("/system/generate_api_key")) {
			
			if(!simpleValidation(new String[] {"email"}, urlParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String inputEmail = urlParameters.get("email");
			Owner owner = dbManager.getOwnerByEmail(inputEmail);
			if(owner == null) {
				int result = dbManager.regOwner(inputEmail);
				
				if(result <= 0) {
					sendHttpResponse(ctx, buildSimpleResponse("Error", "Error occurred during registration", HttpResponseStatus.INTERNAL_SERVER_ERROR, defaultResponseHeaders));
					return;
				}
				
				owner = dbManager.getOwnerByEmail(inputEmail);
			}
			
			String newApiKey = RandomKeyGenerator.nextString(45);
			String newApiSecret = RandomKeyGenerator.nextString(45);
			int added = dbManager.writeNewOwnerSecrets(owner.getId(), newApiKey, newApiSecret);
			
			if(added > 0) {
				EmailSender.send(inputEmail, "New API key generation", "Your secret data: \r\n\r\n API key: "+newApiKey+"\r\nAPI secret: "+newApiSecret);
				responseContent = simpleJsonObject("Success", "API key generated successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while creating the key");
			}
			sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK, defaultResponseHeaders));
		} else if(httpRequest.uri().startsWith("/system/register_game")) {
			Map<String, String> contentParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
			
			if(!simpleValidation(new String[] {"api_key", "game_name", "game_package", "game_type"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			String apiSecret = contentParameters.get("api_secret");
			String gameName = contentParameters.get("game_name");
			String gameJavaPackage = contentParameters.get("game_package");
			String gameType = contentParameters.get("game_type");
			
			if(dbManager.checkGameByKey(apiKey)) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Game alredy exists", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
				return;
			}
			
			OwnerSecrets ownerSecrets = dbManager.getOwnerSecrets(apiKey);
			
			if(ownerSecrets == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while searching for the key", HttpResponseStatus.INTERNAL_SERVER_ERROR, defaultResponseHeaders));
				return;
			}
			
			dbManager.enableTransactions();
			String prefix = GamesDbEngine.generateTablePrefix(gameName, apiKey);
			int added = dbManager.insertGame(gameName, gameJavaPackage, ownerSecrets.getOwnerId(), apiKey, apiSecret, gameType, prefix, generateHmacHash(apiKey, apiSecret));
			
			if(added < 1) {
				dbManager.rollback();
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while adding the game", HttpResponseStatus.INTERNAL_SERVER_ERROR, defaultResponseHeaders));
				return;
			}
			
			dbManager.removeApiKey(apiKey);
			
			dbManager.commit();
			dbManager.disableTransactions();
			
			dbManager.createGameTables(TEMPLATE_1, prefix); //throw exception if not successfully
			String email = dbManager.getOwnerEmailById(ownerSecrets.getOwnerId());
			EmailSender.send(email, "Your game registerd", "Your game \""+gameName+"\" registered with key "+apiKey);
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Register successed", HttpResponseStatus.OK, defaultResponseHeaders));
			
		} /*else if(httpRequest.uri().startsWith("/system/udpate_game_data")) {
			Map<String, String> contentParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
			
			if(!simpleValidation(new String[] {"api_key"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			int updated = dbManager.updateGame(contentParameters.get("new_game_name"), contentParameters.get("new_game_package"));
			// update tables names ???
			if(updated > 0) {
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game data "+contentParameters.get("api_key")+" updated");
			}
			
		}*/ else if(httpRequest.uri().startsWith("/system/delete_game")) {
			if(!simpleValidation(new String[] {"api_key"}, urlParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String apiKey = urlParameters.get("api_key");
			
			Game game = dbManager.deleteGame(apiKey);
			
			if(game == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while deleting the game", HttpResponseStatus.INTERNAL_SERVER_ERROR, defaultResponseHeaders));
				return;
			}
			
			dbManager.deleteGameTables(game.getPrefix(), TEMPLATE_1.getTableNames());
			
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Deletion was successful", HttpResponseStatus.OK, defaultResponseHeaders));
		} else {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
		}
	}
	
	private SqlRequest parseRequest(FullHttpRequest httpRequest, String tableNamePrefix) throws JSONException, SQLException {
		SqlRequest result = null;
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		String tableName = tableNamePrefix+urlParameters.get("table");
		
		if(httpRequest.uri().startsWith("/api/select")) {
			
			List<SqlExpression> whereData = DataBaseInterface.parseWhere(new JSONArray(httpRequest.content().toString(CharsetUtil.UTF_8)));
			result = new SqlSelect(tableName, whereData);
			
		} else if(httpRequest.uri().startsWith("/api/insert")) {
			
			List<CellData> insertData = DataBaseInterface.parseCellDataRow(httpRequest.content().toString(CharsetUtil.UTF_8));
			result = new SqlInsert(tableName, insertData);
			
		} else if(httpRequest.uri().startsWith("/api/update")) {
			
			String jsonUpdateData = httpRequest.content().toString(CharsetUtil.UTF_8);
			JSONObject updateData = new JSONObject(jsonUpdateData);
			
			List<SqlExpression> whereData = DataBaseInterface.parseWhere(updateData.getJSONArray("where"));
			List<CellData> setData = DataBaseInterface.parseCellDataRow(updateData.getJSONArray("set"));
			
			result = new SqlUpdate(tableName, whereData, setData);
			
		}
			
		return result;
		
	}
	
	private void handleApiRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, JSONException {
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key", "table"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		Game game = null;
		
		if(hmac) {
			String inputHash = httpRequest.headers().get("Authorization");
			game = dbManager.getGameByHash(inputHash);
		} else {
			String apiKey = urlParameters.get("api_key");
			game = dbManager.getGameByKey(apiKey);
		}
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
			return;
		}
		
		SqlRequest sqlRequest = parseRequest(httpRequest, game.getPrefix());
		
		if(sqlRequest instanceof SqlInsert && urlParameters.containsKey("updateIfExists") && urlParameters.containsKey("checkField1")) {
			
			Row insertData = new Row(DataBaseInterface.parseCellDataRow(httpRequest.content().toString(CharsetUtil.UTF_8)));
			List<SqlExpression> whereExpression = new ArrayList<>();
			int i = 1;
				
			while (urlParameters.containsKey("checkField"+i)) {
				String fieldName = urlParameters.get("checkField"+(i++));
				if(!insertData.containsCell(fieldName)) {
					sendHttpResponse(ctx, buildSimpleResponse("Error", "Field name mismatch", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
					return;
				}
				whereExpression.add(new SqlExpression(fieldName, insertData.getCell(fieldName).getValue()));
			}
				
			List<List<CellData>> rows = dbManager.executeSelect(new SqlSelect(sqlRequest.getTableName(), whereExpression));
				
			if(rows.size() > 0) {
				List<CellData> updateData = insertData.getCells();
				sqlRequest = new SqlUpdate(sqlRequest.getTableName(), whereExpression, updateData);
			}
		}
		
		if(sqlRequest instanceof SqlSelect) {
			List<List<CellData>> rows = dbManager.executeSelect((SqlSelect) sqlRequest);
			responseContent = rowsToJson(rows);
		} else if(sqlRequest instanceof SqlInsert) {
			int result = dbManager.executeInsert((SqlInsert) sqlRequest);
			if(result > 0) {
				responseContent = simpleJsonObject("Success", "Insert completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while inserting");
			}
		} else if(sqlRequest instanceof SqlUpdate) {
			int result = dbManager.executeUpdate((SqlUpdate) sqlRequest);
			if(result > 0) {
				responseContent = simpleJsonObject("Success", "Update completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while updating");
			}
		} else if(sqlRequest instanceof Increment) {
			boolean result = dbManager.executeIncrement((Increment) sqlRequest);
			if(result) {
				responseContent = simpleJsonObject("Success", "Increment completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while incrementing");
			}
		} else if(sqlRequest instanceof Decrement) {
			int result = dbManager.executeDecrement((Decrement) sqlRequest);
			if(result > 0) {
				responseContent = simpleJsonObject("Success", "Decrement completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while decrementing");
			}
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK, defaultResponseHeaders));
	}
	
	private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus, /*HttpHeaders headers*/Map<String, String> headers) {
		return buildResponse(simpleJsonObject(status, message), httpStatus, headers);
	}
	
	private FullHttpResponse buildResponse(String content, HttpResponseStatus httpStatus, /*HttpHeaders headers*/Map<String, String> headers) {
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpStatus, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		//response.headers().add(headers);
		addResponseHeaders(response, headers);
		response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		return response;
	}
	
	private String httpRequestToString(FullHttpRequest httpRequest) {
		return httpRequest.toString().substring(httpRequest.toString().indexOf("\n")+1)+"\n\n"+httpRequest.content().toString(CharsetUtil.UTF_8);
	}
	
	private void addResponseHeaders(FullHttpResponse httpResponse, Map<String, String> headers) {
		for(Map.Entry<String, String> h : headers.entrySet()) {
			httpResponse.headers().add(h.getKey(), h.getValue());
		}
	}
	
	private Map<String, String> parseParameters(String input) {
		Map<String, String> reuslt = new HashMap<>();
		if(input == null) {
			return reuslt;
		}
		
		String[] pairs = input.split("&");
		
		if(pairs.length < 2 && !input.contains("=")) {
			return reuslt;
		}
		
		for(String pair : pairs) {
			String[] params = pair.split("=");
			
			if(params.length < 2) {
				reuslt.put(params[0], "");
				continue;
			}
			reuslt.put(params[0], params[1]);
		}
		
		return reuslt;
	}
	
	private Map<String, String> parseUrlParameters(String url) {
		Map<String, String> reuslt = new HashMap<>();
		Map<String, List<String>> parameters = new QueryStringDecoder(url).parameters();
		for(Map.Entry<String, List<String>> p : parameters.entrySet()) {
			reuslt.put(p.getKey(), p.getValue().get(0));
		}
		return reuslt;
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
	
	private static GameTemplate createGameTemplate1() {
		
		List<TableTemplate> tblTemplates = new ArrayList<>();
		TableTemplate levelsTemplate = new TableTemplate("levels", new ColData[] {new ColData(Types.INTEGER, "playerId", false),
				  																  new ColData(Types.INTEGER, "level", false),
																				  new ColData(Types.INTEGER, "score"),
																				  new ColData(Types.INTEGER, "stars")});
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));
		
		TableTemplate playersTemplate = new TableTemplate("players", new ColData[] {new ColData(Types.VARCHAR, "playerId", false), 
																					new ColData(Types.INTEGER, "max_level")});
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"playerId"}, true));
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", new ColData[] {new ColData(Types.INTEGER, "playerId", false), 
				  																  new ColData(Types.VARCHAR, "name", false), 
																				  new ColData(Types.INTEGER, "count", "0")});
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "name"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		
		return new GameTemplate("Template1", tblTemplates);
	}
	
	private boolean simpleValidation(String[] names, Map<String, String> parameters) {
		for(String name : names) {
			if(!parameters.containsKey(name) || "".equals(parameters.get(name))) {
				return false;
			}
		}
		return true;
	}
	
	private void sendValidationFailResponse(ChannelHandlerContext ctx) {
		sendHttpResponse(ctx, buildSimpleResponse("Error", "Parameters validation failed", HttpResponseStatus.BAD_REQUEST, defaultResponseHeaders));
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
	
	/*String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}*/
    
	private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse){
		ctx.writeAndFlush(httpResponse);
	}
	
	private String generateHmacHash(String apiKey, String apiSecret) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
		return Arrays.toString(Base64.getEncoder().encode(sha256_HMAC.doFinal(apiKey.getBytes())));
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
}
