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
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Decrement;
import com.my.gamesdataserver.basedbclasses.Increment;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlRequest;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.dbengineclasses.OwnerSecrets;
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
	
	private enum RequestGroup {BASE, API, TEMPLATE_1_API, PLAYER_AUTHORIZATION, ALLOWED_REQUEST, BAD};
	
	private EmailSender emailSender;
	
	static {
		TEMPLATE_1 = createGameTemplate1();
		/*defaultResponseHeaders = new HashMap<>();
		defaultResponseHeaders.put("Access-Control-Allow-Origin", "*");
		defaultResponseHeaders.put("Content-type", "application/json");*/
		/*defaultResponseHeaders.put("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
		defaultResponseHeaders.put("Access-Control-Max-Age", "1000");
		defaultResponseHeaders.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");*/
	}
	
	public ClientHandler(DatabaseConnectionManager dbConnectionPool, LogManager logManager, EmailSender emailSender) throws IOException {
		this.dbConnectionPool = dbConnectionPool;
		//this.template1DbManager = new Template1DbEngine(dbInterface);
		this.logManager = logManager;
		this.emailSender = emailSender;
	}
	
	private RequestGroup recognizeRequestGroup(FullHttpRequest httpRequest) {
		if(httpRequest.uri().startsWith("/api")) {
			return RequestGroup.API;
		} else if(httpRequest.uri().startsWith("/api/template1")) {
			return RequestGroup.TEMPLATE_1_API;
		} else if(httpRequest.uri().startsWith("/player")) {
			return RequestGroup.PLAYER_AUTHORIZATION;
		} else if(httpRequest.uri().startsWith("/system")) {
			return RequestGroup.BASE;
		} else if(httpRequest.uri().startsWith("/special")) {
			return RequestGroup.ALLOWED_REQUEST;
		} else {
			return RequestGroup.BAD;
		}
	}
	
	@Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		RequestGroup requestGroup = recognizeRequestGroup(fullHttpRequest);
		try {
			connection = dbConnectionPool.getConnection();
			switch (requestGroup) {

			case BASE:
				handleSystemRequest(ctx, fullHttpRequest);
				break;
				
			case PLAYER_AUTHORIZATION:
				handlePlayerAuthorization(ctx, fullHttpRequest);
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
			connection = null;
		}
    }
	
	private void handleAllowedRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws SQLException, FileNotFoundException, ClassNotFoundException, IOException, JSONException {
		Game game = authenticationGame(fullHttpRequest);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
			return;
		}
		
		Map<String, AllowedUnconditionalRequest> allowedRequests = readAllowedRequestFile(game.getApiKey());
		
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
		selectRequest.setFields(allowedData.getAllowedFields());
		List<Row> rows = DataBaseMethods.executeSelect(selectRequest, connection);
		String responseContent;
		
		responseContent = Row.rowsToJson(rows);
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
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

	private void handlePlayerAuthorization(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
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
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(httpRequest.uri().startsWith("/system/generate_api_key")) {
			
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
		} else if(httpRequest.uri().startsWith("/system/register_game")) {
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
			
			List<AllowedUnconditionalRequest> allowedRequest = new ArrayList<>();
			allowedRequest.add(new AllowedUnconditionalRequest(apiKey, "players", "players", new String[] {"facebookId", "maxLevel"}));
			addAllowedRequestsToFile(allowedRequest, apiKey);
			
			String email = DataBaseMethods.getOwnerEmailById(ownerSecrets.getOwnerId(), connection);
			emailSender.send(email, "Your game registerd", "Your game \""+gameName+"\" registered with key "+apiKey);
			
			connection.commit();
			connection.setAutoCommit(true);
			
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Register successed", HttpResponseStatus.OK));
			
		} else if(httpRequest.uri().startsWith("/system/add_special_request")) {
			
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
			
			List<AllowedUnconditionalRequest> allowedRequest = new ArrayList<>();
			allowedRequest.add(new AllowedUnconditionalRequest(game.getApiKey(), requestName, tableName, fieldsArray));
			addAllowedRequestsToFile(allowedRequest, game.getApiKey());
			
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Request added successfully", HttpResponseStatus.OK));
			
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
			
			Game game = DataBaseMethods.deleteGame(apiKey, connection);
			
			if(game == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while deleting the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			
			DataBaseMethods.deleteGameTables(game.getPrefix(), TEMPLATE_1.getTableNames(), connection);
			
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Deletion was successful", HttpResponseStatus.OK));
		} else {
		
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
		}
	}
	
	private SqlRequest parseRequest(FullHttpRequest httpRequest, String tableNamePrefix) throws JSONException, SQLException {
		SqlRequest result = null;
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		String tableName = tableNamePrefix+urlParameters.get("table");
		
		if(httpRequest.uri().startsWith("/api/select")) {
			
			List<SqlExpression> whereData = SqlMethods.parseWhere(new JSONArray(httpRequest.content().toString(CharsetUtil.UTF_8)));
			result = new SqlSelect(tableName, whereData);
			
		} else if(httpRequest.uri().startsWith("/api/insert")) {
			
			List<CellData> insertData = SqlMethods.parseCellDataRow(httpRequest.content().toString(CharsetUtil.UTF_8));
			result = new SqlInsert(tableName, insertData);
			
		} else if(httpRequest.uri().startsWith("/api/update")) {
			
			String jsonUpdateData = httpRequest.content().toString(CharsetUtil.UTF_8);
			JSONObject updateData = new JSONObject(jsonUpdateData);
			
			List<SqlExpression> whereData = SqlMethods.parseWhere(updateData.getJSONArray("where"));
			List<CellData> setData = SqlMethods.parseCellDataRow(updateData.getJSONArray("set"));
			
			result = new SqlUpdate(tableName, whereData, setData);
			
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
		SqlRequest sqlRequest = parseRequest(httpRequest, game.getPrefix());
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		String playerIdFieldName = game.getPlayerIdFieldName()!=null?game.getPlayerIdFieldName():defaultPlayerIdFieldName;		   //
		String playerId = httpRequest.headers().get(Authorization.PLAYER_ID_HEADER);											   //
		if(sqlRequest instanceof SqlSelect || sqlRequest instanceof SqlUpdate) {												   //
			sqlRequest.addExpression(new SqlExpression(playerIdFieldName, playerId));											   //
		} else if(sqlRequest instanceof SqlInsert) {																			   //
			((SqlInsert) sqlRequest).addInsertedValue(new CellData(playerIdFieldName, playerId));								   //
		}																														   //
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		if(sqlRequest instanceof SqlInsert && urlParameters.containsKey("updateIfExists") && urlParameters.containsKey("checkField1")) {
			
			Row insertData = new Row(((SqlInsert) sqlRequest).getRowToInsert(0)/*SqlMethods.parseCellDataRow(httpRequest.content().toString(CharsetUtil.UTF_8))*/);
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
		}
		
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
		} /*else if(sqlRequest instanceof Increment) {
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
		}*/
		
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
		TableTemplate levelsTemplate = new TableTemplate("levels", new ColData[] {new ColData(Types.VARCHAR, "playerId").setNull(false),
				  																  new ColData(Types.INTEGER, "level").setNull(false),
																				  new ColData(Types.INTEGER, "score").setNull(false),
																				  new ColData(Types.INTEGER, "stars").setNull(false)});
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));
		
		TableTemplate playersTemplate = new TableTemplate("players", new ColData[] {new ColData(Types.VARCHAR, "playerId").setNull(false),
																					new ColData(Types.VARCHAR, "facebookId").setNull(false),
																					new ColData(Types.INTEGER, "maxLevel").setDefaultValue("0").setNull(false)}, "playerId");
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"facebookId"}, true));
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", new ColData[] {new ColData(Types.VARCHAR, "playerId").setNull(false), 
				  																  new ColData(Types.VARCHAR, "boostName").setNull(false), 
																				  new ColData(Types.INTEGER, "count").setDefaultValue("0")});
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "boostName"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		
		return new GameTemplate("Template1", tblTemplates, new ArrayList<>());
	}
	
	private void addAllowedRequestsToFile(List<AllowedUnconditionalRequest> allowedUnconditionalRequest, String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
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
	}
	
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
	
	/*String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}*/
    
	private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse){
		ctx.writeAndFlush(httpResponse);
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
}
