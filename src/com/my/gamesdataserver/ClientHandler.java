package com.my.gamesdataserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import javax.print.attribute.standard.MediaSize.Engineering;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.SqlExpression;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Field;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlRequest;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.dbengineclasses.OwnerSecrets;
import com.my.gamesdataserver.dbengineclasses.SpecialRequest;
import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;
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
	private DatabaseConnectionManager dbConnectionPool;
	private Connection connection;
	//private GamesDbEngine dbManager;
	private Template1DbEngine template1DbManager;
	private LogManager logManager;
	private String errorLogFilePrefix = "error";
	private static final GameTemplate TEMPLATE_1;
	//private static Map<String, String> defaultResponseHeaders;
	private final String defaultPlayerIdFieldName = "playerId";
	
	private enum RequestGroup {BASE, API, TEMPLATE_1_API, PLAYER_REQUEST, ALLOWED_REQUEST, BAD};
	private enum RequestName {GEN_API_KEY, REGISTER_GAME, CREATE_SPEC_REQUEST, DELETE_GAME, PLAYER_AUTHORIZATION, SELECT, INSERT, UPDATE};
	
	private static Map<String, RequestGroup> requestGroupMap;
	private static Pattern requestGroupPattern;
	private static Map<String, RequestName> requestMap;
	private static Pattern requestNamePattern;
	

	private static Pattern specialRequestNamePattern;
	
	private EmailSender emailSender;
	
	static {
		TEMPLATE_1 = createGameTemplate1();
		/*defaultResponseHeaders = new HashMap<>();
		defaultResponseHeaders.put("Access-Control-Allow-Origin", "*");
		defaultResponseHeaders.put("Content-type", "application/json");*/
		/*defaultResponseHeaders.put("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
		defaultResponseHeaders.put("Access-Control-Max-Age", "1000");
		defaultResponseHeaders.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");*/
		
		requestGroupMap = new HashMap<>();
		requestGroupMap.put("api", RequestGroup.API);
		requestGroupMap.put("player", RequestGroup.PLAYER_REQUEST);
		requestGroupMap.put("system", RequestGroup.BASE);
		requestGroupMap.put("special", RequestGroup.ALLOWED_REQUEST);
		
		requestGroupPattern = Pattern.compile("^\\/(\\w+)\\/?");
		
		requestMap = new HashMap<>();
		requestMap.put("/system/generate_api_key", RequestName.GEN_API_KEY);
		requestMap.put("/system/register_game", RequestName.REGISTER_GAME);
		requestMap.put("/system/add_special_request", RequestName.CREATE_SPEC_REQUEST);
		requestMap.put("/system/delete_game", RequestName.DELETE_GAME);
		requestMap.put("/player/authorization", RequestName.PLAYER_AUTHORIZATION);
		requestMap.put("/api/select", RequestName.SELECT);
		requestMap.put("/api/insert", RequestName.INSERT);
		requestMap.put("/api/update", RequestName.UPDATE);
		
		requestNamePattern = Pattern.compile("^\\/\\w+\\/\\w+");
		
		specialRequestNamePattern = Pattern.compile("^\\/\\w+\\/\\w+\\/(\\w+)(\\?|\\/)");
	}
	
	public ClientHandler(DatabaseConnectionManager dbConnectionPool, LogManager logManager, EmailSender emailSender) throws IOException {
		this.dbConnectionPool = dbConnectionPool;
		//this.template1DbManager = new Template1DbEngine(dbInterface);
		this.logManager = logManager;
		this.emailSender = emailSender;
	}
	
	private RequestGroup recognizeRequestGroup(String urlPath) {
		Matcher requestGroupMatcher = requestGroupPattern.matcher(urlPath);
		if(requestGroupMatcher.find()) return requestGroupMap.get(requestGroupMatcher.group(1));
		return RequestGroup.BAD;
	}
	
	private RequestName recognizeRequestName(String urlPath) {
		Matcher requestNameMatcher = requestNamePattern.matcher(urlPath);
		if(requestNameMatcher.find()) return requestMap.get(requestNameMatcher.group());
		return null;
	}
	
	private String recognizeSpecialRequestName(String urlPath) {
		Matcher requestNameMatcher = specialRequestNamePattern.matcher(urlPath);
		if(requestNameMatcher.find()) return requestNameMatcher.group(1);
		return null;
	}
	
	@Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		RequestGroup requestGroup = recognizeRequestGroup(fullHttpRequest.uri());
		try {
			connection = dbConnectionPool.getConnection();
			switch (requestGroup) {

			case BASE:
				handleSystemRequest(ctx, fullHttpRequest);
				break;
				
			case PLAYER_REQUEST:
				handlePlayerRequest(ctx, fullHttpRequest);
				break;
				
			case API:
				handleApiRequest(ctx, fullHttpRequest);
				break;
				
			case TEMPLATE_1_API:
				handleTemplateRequest(ctx, fullHttpRequest);
				break;
				
			case ALLOWED_REQUEST:
				handleAllowedRequest(ctx, fullHttpRequest);
				break;
				
			case BAD:
				FullHttpResponse httpResponse = buildSimpleResponse("Error", "Bad request group", HttpResponseStatus.BAD_REQUEST);
				logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), "ERROR: Bad request group".toUpperCase(), httpResponse.toString());
				sendHttpResponse(ctx, httpResponse);
				break;
			}
			
		} catch (JSONException | SQLException | MessagingException | InvalidKeyException | NoSuchAlgorithmException | IOException | ClassNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			try {
				if(connection != null && !connection.getAutoCommit()) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace(pw);
			}
			
			e.printStackTrace(pw);
			FullHttpResponse httpResponse = buildSimpleResponse("Error", "An error occurred during processing", HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), sw.toString(), httpResponse.toString());
			sendHttpResponse(ctx, httpResponse);
		} finally {
			try {
				if(connection != null) connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
    }
	
	private void handleAllowedRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws SQLException, FileNotFoundException, ClassNotFoundException, IOException, JSONException {
		Game game = authenticationGame(fullHttpRequest);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
			return;
		}
		
		/*Map<String, AllowedUnconditionalRequest> allowedRequests = readAllowedRequestFile(game.getApiKey());
		
		String[] uriParts = fullHttpRequest.uri().split("/");

		if(uriParts.length < 3) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Error during processing", HttpResponseStatus.OK));
			return;
		}
		
		if(!allowedRequests.containsKey(uriParts[2])) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Request not allowed", HttpResponseStatus.OK));
			return;
		}

		AllowedUnconditionalRequest allowedData = allowedRequests.get(uriParts[2]);
		List<SqlExpression> whereData = SqlMethods.parseWhere(new JSONArray(fullHttpRequest.content().toString(CharsetUtil.UTF_8)));
		SqlSelect selectRequest = new SqlSelect(game.getPrefix()+allowedData.getTableName(), whereData);
		selectRequest.setFields(allowedData.getAllowedFields());*/
		
		List<SqlExpression> whereData = SqlMethods.parseWhere(new JSONArray(fullHttpRequest.content().toString(CharsetUtil.UTF_8)));
		SpecialRequest sr = DataBaseMethods.readSpecialRequest(game.getId(), recognizeSpecialRequestName(fullHttpRequest.uri()), connection);
		List<Row> rows = DataBaseMethods.executeSpecialRequest(sr, whereData, connection);
		
		sendHttpResponse(ctx, buildResponse(Row.rowsToJson(rows), HttpResponseStatus.OK));
	}

	private Game authenticationGame(FullHttpRequest httpRequest) throws SQLException {
		
		String authorization = httpRequest.headers().get("Authorization");
		
		if(authorization != null && !"".equals(authorization) && authorization.contains(":")) {
			String inputGameHash = authorization.substring(authorization.indexOf(":")+1);
			return DataBaseMethods.getGameByHash(inputGameHash, connection);
		}

		String apiKey = httpRequest.headers().get("API_key");
		
		if(apiKey == null || "".equals(apiKey)) {
			return DataBaseMethods.getGameByKey(apiKey, connection);
		}
		
		return null;
	}

	private void handlePlayerRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
		Map<String, String> urlParameters = parseUrlParameters(fullHttpRequest.uri());
		if(fullHttpRequest.uri().startsWith("/player/authorization")) {
			
			if(!simpleValidation(new String[] {"facebookId"}, urlParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			Authorization auth = new Authorization();
			if(!auth.requestAuthentication(fullHttpRequest)) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
				return;
			}
			
			Game game = authenticationGame(fullHttpRequest);
			
			if(game == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
				return;
			}
			
			String facebookId = urlParameters.get("facebookId");
			Player player = DataBaseMethods.getPlayerByFacebookId(facebookId, game.getPrefix(), connection);
			
			if(player != null) {
				sendHttpResponse(ctx, buildSimpleResponse("playerId", player.getPlayerId(), HttpResponseStatus.OK));
				return;
			}
			
			String playerId = DataBaseMethods.registrationPlayerByFacebookId(urlParameters.get("facebookId"), game.getPrefix(), connection);
			if(playerId != null) {
				sendHttpResponse(ctx, buildSimpleResponse("playerId", playerId, HttpResponseStatus.OK));
			} else {
				sendValidationFailResponse(ctx);
			}
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
		
		Game game = DataBaseMethods.getGameByKey(apiKey, connection);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		template1DbManager.setTablePrefix(game.getPrefix());
		
		String playerId = urlParameters.get("playerId");
		
		Player player = template1DbManager.getPlayer(playerId);
		
		if(player == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Player not found", HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		if(httpRequest.uri().startsWith("/api/template1/completeLevel")) {
			
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
	}

	private void handleSystemRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, MessagingException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, IOException, ClassNotFoundException {
		
		RequestName requestName = recognizeRequestName(httpRequest.uri());
		
		switch (requestName) {
		case GEN_API_KEY:
			handleGenApiKey(ctx, httpRequest);
			break;
		case REGISTER_GAME:
			handleRegisterGame(ctx, httpRequest);
			break;
		case CREATE_SPEC_REQUEST:
			handleSpecailRequest(ctx, httpRequest);
			break;
		case DELETE_GAME:
			handleDeleteGame(ctx, httpRequest);
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
	}
	
	private void handleDeleteGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException {
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String apiKey = urlParameters.get("api_key");
		
		Game game = DataBaseMethods.deleteGame(apiKey, connection);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while deleting the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		DataBaseMethods.deleteGameTables(game.getPrefix(), TEMPLATE_1.getTableNames(), connection);
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Deletion was successful", HttpResponseStatus.OK));
	}

	private void handleSpecailRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, FileNotFoundException, ClassNotFoundException, IOException {
		Game game = authenticationGame(httpRequest);
		
		/*Authorization auth = new Authorization();
		
		if(!auth.authorization(httpRequest, game, connection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
			return;
		}*/
		
		Map<String, String> contentParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!simpleValidation(new String[] {"request_name", "table", "fields_set"}, contentParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String requestName = contentParameters.get("request_name");
		String tableName = contentParameters.get("table");
		String fields = contentParameters.get("fields_set");
		
		String[] fieldsArray = fields.split(",");
		for(String field : fieldsArray) {
			field = field.trim();
		}
		
		/*List<AllowedUnconditionalRequest> allowedRequest = new ArrayList<>();
		allowedRequest.add(new AllowedUnconditionalRequest(game.getApiKey(), requestName, tableName, fieldsArray));
		addAllowedRequestsToFile(allowedRequest, game.getApiKey());*/
		DataBaseMethods.addSpecialRequest(game.getId(), requestName, tableName, fields, connection);
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Request added successfully", HttpResponseStatus.OK));
	}

	private void handleRegisterGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException {
		Map<String, String> contentParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!simpleValidation(new String[] {"api_key", "game_name", "game_package", "game_type"}, contentParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String apiKey = contentParameters.get("api_key");
		
		String gameName = contentParameters.get("game_name");
		String gameJavaPackage = contentParameters.get("game_package");
		String gameType = contentParameters.get("game_type");
		
		if(DataBaseMethods.checkGameByKey(apiKey, connection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game alredy exists", HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		OwnerSecrets ownerSecrets = DataBaseMethods.getOwnerSecrets(apiKey, connection);
		
		if(ownerSecrets == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while searching for the key", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		connection.setAutoCommit(false);
		String prefix = DataBaseMethods.generateTablePrefix(gameName, apiKey);
		String apiSecret = ownerSecrets.getApiSecret();
		int added = DataBaseMethods.insertGame(gameName, gameJavaPackage, ownerSecrets.getOwnerId(), apiKey, apiSecret, gameType, prefix, Authorization.generateHmacHash(apiSecret, apiKey), connection);
		
		if(added < 1) {
			connection.rollback();
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while adding the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		DataBaseMethods.removeApiKey(apiKey, connection);
		
		DataBaseMethods.createGameTables(TEMPLATE_1, prefix, connection); //throw exception if not successfully
		
		/*List<AllowedUnconditionalRequest> allowedRequest = new ArrayList<>();
		allowedRequest.add(new AllowedUnconditionalRequest(apiKey, "players", "players", new String[] {"facebookId", "maxLevel"}));
		addAllowedRequestsToFile(allowedRequest, apiKey);*/
		Game game = DataBaseMethods.getGameByKey(apiKey, connection);
		DataBaseMethods.addSpecialRequest(game.getId(), "players", "players", "*", connection);
		DataBaseMethods.addSpecialRequest(game.getId(), "levels", "levels", "*", connection);
		
		String email = DataBaseMethods.getOwnerEmailById(ownerSecrets.getOwnerId(), connection);
		emailSender.send(email, "Your game registerd", "Your game \""+gameName+"\" registered with key "+apiKey);
		
		connection.commit();
		connection.setAutoCommit(true);
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Register successed", HttpResponseStatus.OK));
	}

	private void handleGenApiKey(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException {
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		
		if(!simpleValidation(new String[] {"email"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String inputEmail = urlParameters.get("email");
		Owner owner = DataBaseMethods.getOwnerByEmail(inputEmail, connection);
		if(owner == null) {
			int result = DataBaseMethods.regOwner(inputEmail, connection);
			
			if(result <= 0) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Error occurred during registration", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			
			owner = DataBaseMethods.getOwnerByEmail(inputEmail, connection);
		}
		
		String newApiKey = RandomKeyGenerator.nextString(24);
		String newApiSecret = RandomKeyGenerator.nextString(45);
		int added = DataBaseMethods.writeNewOwnerSecrets(owner.getId(), newApiKey, newApiSecret, connection);
		
		if(added > 0) {
			emailSender.send(inputEmail, "New API key generation", "Your secret data: \r\n\r\n API key: "+newApiKey+"\r\nAPI secret: "+newApiSecret);
			responseContent = simpleJsonObject("Success", "API key generated successfully");
		} else {
			responseContent = simpleJsonObject("Error", "An error occurred while creating the key");
		}
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
	}

	private SqlRequest buildRequest(RequestName requestName, String data, String tableName) throws JSONException, SQLException {
		SqlRequest result = null;
		List<SqlExpression> whereData;
		switch (requestName) {
		case SELECT:
			whereData = SqlMethods.parseWhere(new JSONArray(data));
			result = new SqlSelect(tableName, whereData);
			break;
		case INSERT:
			List<SqlExpression> insertData = SqlMethods.parseCellDataRow(data);
			result = new SqlInsert(tableName, insertData);
			break;
		case UPDATE:
			JSONObject updateData = new JSONObject(data);
			
			whereData = SqlMethods.parseWhere(updateData.getJSONArray("where"));
			List<SqlExpression> setData = SqlMethods.parseCellDataRow(updateData.getJSONArray("set"));
			
			result = new SqlUpdate(tableName, whereData, setData);
			break;
		default:
			return null;
		}
			
		return result;
		
	}
	
	private void handleApiRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
		String responseContent = "";
		/*Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key", "table"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}*/
		
		Game game = authenticationGame(httpRequest);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
			return;
		}
		
		Authorization auth = new Authorization();
		
		if(!auth.authorization(httpRequest, game, connection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
			return;
		}
		
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		SqlRequest sqlRequest = buildRequest(recognizeRequestName(httpRequest.uri()), httpRequest.content().toString(CharsetUtil.UTF_8), game.getPrefix()+urlParameters.get("table"));
		
		// Player id add to input request always ////////////////////////////////////////////////////////////////////////////////////
		String playerIdFieldName = game.getPlayerIdFieldName()!=null?game.getPlayerIdFieldName():defaultPlayerIdFieldName;		   //
		String playerId = httpRequest.headers().get(Authorization.PLAYER_ID_HEADER);											   //
		if(sqlRequest instanceof SqlSelect || sqlRequest instanceof SqlUpdate) {												   //
			sqlRequest.addExpression(new SqlExpression(playerIdFieldName, playerId));											   //
		} else if(sqlRequest instanceof SqlInsert) {																			   //
			((SqlInsert) sqlRequest).addInsertedValue(new SqlExpression(playerIdFieldName, playerId));								   //
		}																														   //
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		/*if(sqlRequest instanceof SqlInsert && urlParameters.containsKey("updateIfExists") && urlParameters.containsKey("checkField1")) {
			
			Row insertData = new Row(((SqlInsert) sqlRequest).getRowToInsert(0)*//*SqlMethods.parseCellDataRow(httpRequest.content().toString(CharsetUtil.UTF_8))*//*);
			List<SqlExpression> insertData = ((SqlInsert) sqlRequest).getRowToInsert(0);
			List<SqlExpression> whereExpression = new ArrayList<>();
			int i = 1;
				
			while (urlParameters.containsKey("checkField"+i)) {
				String fieldName = urlParameters.get("checkField"+(i++));
				if(!insertData.containsCell(fieldName)) {
					sendHttpResponse(ctx, buildSimpleResponse("Error", "Field name mismatch", HttpResponseStatus.BAD_REQUEST));
					return;
				}
				whereExpression.add(new SqlExpression(fieldName, insertData.getCell(fieldName).getValue()));
			}
				
			List<Row> rows = DataBaseMethods.executeSelect(new SqlSelect(sqlRequest.getTableName(), whereExpression), connection);
				
			if(rows.size() > 0) {
				sqlRequest = new SqlUpdate(sqlRequest.getTableName(), whereExpression, insertData.toList());
			}
		}*/
		
		if(sqlRequest instanceof SqlSelect) {
			List<Row> rows = DataBaseMethods.executeSelect((SqlSelect) sqlRequest, connection);
			
			//responseContent = new Row(rows.get(0)).removeCell(playerIdFieldName).toJson();
			if(rows.size() > 0) {
				//responseContent = rows.get(0).removeCell(playerIdFieldName).toJson();
				for(Row row : rows) {
					row.removeCell(playerIdFieldName);
				}
				responseContent = Row.rowsToJson(rows);
			} else {
				responseContent = "[]";
			}
		} else if(sqlRequest instanceof SqlInsert) {
			
			SqlInsert insertRequest = (SqlInsert)sqlRequest;
			
			int result = DataBaseMethods.executeInsert((SqlInsert) sqlRequest, connection);
			if(result > 0) {
				responseContent = simpleJsonObject("Success", "Insert completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while inserting");
			}
		} else if(sqlRequest instanceof SqlUpdate) {
			int result = DataBaseMethods.executeUpdate((SqlUpdate) sqlRequest, connection);
			if(result > 0) {
				responseContent = simpleJsonObject("Success", "Update completed successfully");
			} else {
				responseContent = simpleJsonObject("Error", "An error occurred while updating");
			}
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
	}
	
	private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus, /*HttpHeaders headers*/Map<String, String> headers) {
		return buildResponse(simpleJsonObject(status, message), httpStatus, headers);
	}
	
	private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus) {
		return buildResponse(simpleJsonObject(status, message), httpStatus);
	}
	
	private FullHttpResponse buildResponse(String content, HttpResponseStatus httpStatus, /*HttpHeaders headers*/Map<String, String> headers) {
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpStatus, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		//response.headers().add(headers);
		addResponseHeaders(response, headers);
		response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		return response;
	}
	
	private FullHttpResponse buildResponse(String content, HttpResponseStatus httpStatus) {
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpStatus, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		response.headers().set("Content-Type", "application/json");
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
	
	
	
	private static GameTemplate createGameTemplate1() {
		
		List<TableTemplate> tblTemplates = new ArrayList<>();
		TableTemplate levelsTemplate = new TableTemplate("levels", new Field[] {new Field(Types.VARCHAR, "playerId").setNull(false),
				  																  new Field(Types.INTEGER, "level").setNull(false),
																				  new Field(Types.INTEGER, "score").setNull(false),
																				  new Field(Types.INTEGER, "stars").setNull(false)});
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));
		
		TableTemplate playersTemplate = new TableTemplate("players", new Field[] {new Field(Types.VARCHAR, "playerId").setNull(false),
																					new Field(Types.VARCHAR, "facebookId").setNull(false),
																					new Field(Types.INTEGER, "maxLevel").setDefaultValue("0").setNull(false)}, "playerId");
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"facebookId"}, true));
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", new Field[] {new Field(Types.VARCHAR, "playerId").setNull(false), 
				  																  new Field(Types.VARCHAR, "boostName").setNull(false), 
																				  new Field(Types.INTEGER, "count").setDefaultValue("0")});
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "boostName"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		
		return new GameTemplate("Template1", tblTemplates, new ArrayList<>());
	}
	
	/*private void addAllowedRequestsToFile(List<AllowedUnconditionalRequest> allowedUnconditionalRequest, String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
		File file = new File(new File(".").getCanonicalPath()+File.separator+"games_settings"+File.separator+fileName+".settings");

		List<AllowedUnconditionalRequest> finalData = allowedUnconditionalRequest;
		
		if(!file.exists()) {
			file.createNewFile();
		} else {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			finalData = (List<AllowedUnconditionalRequest>) ois.readObject();
			ois.close();
			finalData.addAll(allowedUnconditionalRequest);
		}
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(finalData);
		oos.close();
	}*/
	
	private Map<String, AllowedUnconditionalRequest> readAllowedRequestFile(String apiKey) throws FileNotFoundException, IOException, ClassNotFoundException {
		File gameSettingsDir = new File(new File(".").getCanonicalPath()+File.separator+"games_settings");
		if(!gameSettingsDir.exists()) {
			gameSettingsDir.mkdirs();
		}
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(gameSettingsDir.getCanonicalPath()+File.separator+apiKey+".settings"));
		List<AllowedUnconditionalRequest> allowedUnconditionalRequest = (List<com.my.gamesdataserver.AllowedUnconditionalRequest>) ois.readObject();
		Map<String, AllowedUnconditionalRequest> result = new HashMap<>();
		for(AllowedUnconditionalRequest o : allowedUnconditionalRequest) {
			result.put(o.getRequestName(), o);
		}
		return result;
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
		sendHttpResponse(ctx, buildSimpleResponse("Error", "Parameters validation failed", HttpResponseStatus.BAD_REQUEST));
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
	private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse){
		ctx.writeAndFlush(httpResponse);
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
}
