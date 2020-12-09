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

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Field;
import com.my.gamesdataserver.basedbclasses.QueryTypedValue;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.TableIndex;
import com.my.gamesdataserver.basedbclasses.TableTemplate;
import com.my.gamesdataserver.basedbclasses.queryclasses.Select;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.dbengineclasses.SpecialRequest;
import com.my.gamesdataserver.dbengineclasses.ApiMethods;
import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;
import com.my.gamesdataserver.dbengineclasses.Owner;
import com.my.gamesdataserver.dbengineclasses.PlayerId;
import com.my.gamesdataserver.helpers.EmailSender;
import com.my.gamesdataserver.helpers.LogManager;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.BoostsUpdate;
import com.my.gamesdataserver.template1classes.LevelsUpdate;
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
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.CharsetUtil;

public class ClientHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private DatabaseConnectionManager dbConnectionPool;
	private Connection dbConnection;
	//private GamesDbEngine dbManager;
	private Template1DbEngine template1DbManager;
	private LogManager logManager;
	private String errorLogFilePrefix = "error";
	private static final GameTemplate MATCH_3_TEMPLATE;
	//private static Map<String, String> defaultResponseHeaders;
	private final String defaultPlayerIdFieldName = "playerId";
	
	private enum RequestGroup {BASE, API, TEMPLATE_1_API, PLAYER_REQUEST, ALLOWED_REQUEST, BAD, GAME};
	
	private enum RequestName {
		GEN_API_KEY, 
		REGISTER_GAME, 
		CREATE_SPEC_REQUEST, 
		SPEC_REQUEST_LIST, 
		DELETE_GAME, 
		PLAYER_AUTHORIZATION, 
		SELECT, 
		INSERT, 
		UPDATE, 
		LEVEL, 
		BOOST,
		LEADBOARD,
		PLAYERPROGRESS,
		MAXPLAYERPROGRESS};
	
	private static Map<String, RequestGroup> requestGroupMap;
	private static Pattern requestGroupPattern;
	private static Map<String, RequestName> requestMap;
	private static Pattern requestNamePattern;
	

	private static Pattern specialRequestNamePattern;
	
	private EmailSender emailSender;
	
	static {
		MATCH_3_TEMPLATE = match3Template();
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
		requestGroupMap.put("game", RequestGroup.GAME);
		
		requestGroupPattern = Pattern.compile("^\\/(\\w+)\\/?");
		
		requestMap = new HashMap<>();
		requestMap.put("/system/register_game", RequestName.REGISTER_GAME);
		requestMap.put("/system/set_special_request", RequestName.CREATE_SPEC_REQUEST);
		requestMap.put("/system/special_request_list", RequestName.SPEC_REQUEST_LIST);
		requestMap.put("/system/delete_game", RequestName.DELETE_GAME);
		requestMap.put("/player/authorization", RequestName.PLAYER_AUTHORIZATION);
		requestMap.put("/api/select", RequestName.SELECT);
		requestMap.put("/api/insert", RequestName.INSERT);
		requestMap.put("/api/update", RequestName.UPDATE);
		requestMap.put("/game/levels", RequestName.LEVEL);
		requestMap.put("/game/boosts", RequestName.BOOST);
		requestMap.put("/game/leaderboard", RequestName.LEADBOARD);
		requestMap.put("/game/playerprogress", RequestName.PLAYERPROGRESS);
		requestMap.put("/game/maxplayerprogress", RequestName.MAXPLAYERPROGRESS);
		
		requestNamePattern = Pattern.compile("^\\/\\w+\\/\\w+");
		
		specialRequestNamePattern = Pattern.compile("^\\/\\w+\\/(\\w+)(\\?|\\/)?");
	}
	
	public ClientHandler(DatabaseConnectionManager dbConnectionPool, LogManager logManager, EmailSender emailSender) throws IOException {
		this.dbConnectionPool = dbConnectionPool;
		//this.template1DbManager = new Template1DbEngine(dbInterface);
		this.logManager = logManager;
		this.emailSender = emailSender;
	}
	
	private RequestGroup requestGroup(String uri) {
		Matcher requestGroupMatcher = requestGroupPattern.matcher(uri);
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
	
	private boolean isGameDependsRequest(RequestGroup requestGroup) {
		return requestGroup == RequestGroup.PLAYER_REQUEST || requestGroup == RequestGroup.API            || 
			   requestGroup == RequestGroup.GAME           || requestGroup == RequestGroup.TEMPLATE_1_API || 
			   requestGroup == RequestGroup.ALLOWED_REQUEST;
	}
	
	@Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		RequestGroup requestGroup = requestGroup(fullHttpRequest.uri());

		try {
			dbConnection = dbConnectionPool.getConnection();
			
			Game game = parseGame(fullHttpRequest);
			
			if(isGameDependsRequest(requestGroup) && game == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
				return;
			}
			
			Authorization auth = new Authorization();
			
			if(requestGroup == RequestGroup.PLAYER_REQUEST && !auth.requestAuthentication(fullHttpRequest)) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
				return;
			}
			
			if(requestGroup != RequestGroup.BASE && requestGroup != RequestGroup.PLAYER_REQUEST && !auth.authorization(fullHttpRequest, game, dbConnection)) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
				return;
			}
			
			switch (requestGroup) {
			
			case BASE:
				handleSystemRequest(ctx, fullHttpRequest);
				break;
			
			case PLAYER_REQUEST:
				handlePlayerRequest(ctx, fullHttpRequest, game);
				break;
				
			case API:
				handleApiRequest(ctx, fullHttpRequest, game);
				break;
				
			case GAME:
				handleGameRequest(ctx, fullHttpRequest, game);
				break;
				
			case TEMPLATE_1_API:
				handleTemplateRequest(ctx, fullHttpRequest, game);
				break;
				
			case ALLOWED_REQUEST:
				handleAllowedRequest(ctx, fullHttpRequest, game);
				break;
				
			case BAD:
				FullHttpResponse httpResponse = buildSimpleResponse("Error", "Bad request group", HttpResponseStatus.BAD_REQUEST);
				logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), "ERROR: Bad request group".toUpperCase(), httpResponse.toString());
				sendHttpResponse(ctx, httpResponse);
				break;
				
			default:
				FullHttpResponse httpResponse2 = buildSimpleResponse("Error", "Bad request group", HttpResponseStatus.BAD_REQUEST);
				logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), "ERROR: Bad request group".toUpperCase(), httpResponse2.toString());
				sendHttpResponse(ctx, httpResponse2);
				break;
			}
		} catch (JSONException | SQLException | MessagingException | InvalidKeyException | NoSuchAlgorithmException | IOException | ClassNotFoundException e) {
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
			FullHttpResponse httpResponse = buildSimpleResponse("Error", e.getMessage()/*"An error occurred during processing"*/, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			logManager.log(errorLogFilePrefix, httpRequestToString(fullHttpRequest), sw.toString(), httpResponse.toString());
			sendHttpResponse(ctx, httpResponse);
		} finally {
			try {
				if(dbConnection != null) dbConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
    }
	
	private void handleGameRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Game game) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, JSONException {
		
		String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
		
		PlayerId playerId = new PlayerId("playerId", fullHttpRequest.headers().get(Authorization.PLAYER_ID_HEADER));
		
		RequestName requestName = recognizeRequestName(fullHttpRequest.uri());
		
		switch (requestName) {
		case LEVEL:
			JSONArray jsonLevels = new JSONArray(inputContent);
			
			StringBuilder inputLevels = new StringBuilder();
			
			for (int i = 0; i < jsonLevels.length(); i++) {
				inputLevels.append(jsonLevels.getJSONObject(i).getInt("level"));
				if(i<jsonLevels.length()-1) inputLevels.append(",");
			}
			
			List<Row> exsistingLevels = SqlMethods.select("SELECT * FROM "+game.getPrefix()+"levels WHERE "+playerId.getFieldName()+"='"+playerId.getValue()+"' AND level IN("+inputLevels+")", dbConnection);
			
			LevelsUpdate levelsupdate = new LevelsUpdate(jsonLevels, exsistingLevels, playerId, game.getPrefix()+"levels");
			
			int lvlUpdated = 0;
			int lvlIserted = 0;
			
			for(String updateRequest : levelsupdate.getUpdateRequests()) {
				lvlUpdated = SqlMethods.update(updateRequest, dbConnection);
			}
			
			if(levelsupdate.isNeedInsert())
				lvlIserted = SqlMethods.insert(levelsupdate.getInsertRequest(), dbConnection);
			
			sendHttpResponse(ctx, buildResponse("{udpated : "+lvlUpdated+", inserted: "+lvlIserted+"}", HttpResponseStatus.OK));
			break;
		case BOOST:
			JSONArray jsonBoosts = new JSONArray(inputContent);
			
			StringBuilder inputBoostsNames = new StringBuilder();
			
			for (int i = 0; i < jsonBoosts.length(); i++) {
				inputBoostsNames.append("'"+jsonBoosts.getJSONObject(i).getString("name")+"'");
				if(i<jsonBoosts.length()-1) inputBoostsNames.append(",");
			}

			List<Row> exsistingBoosts = SqlMethods.select("SELECT * FROM "+game.getPrefix()+"boosts WHERE "+playerId.getFieldName()+"='"+playerId.getValue()+"' AND name IN("+inputBoostsNames+")", dbConnection);
			
			BoostsUpdate boostsUpdate = new BoostsUpdate(jsonBoosts, exsistingBoosts, playerId, game.getPrefix()+"boosts");
			
			int bstUpdated = 0;
			int bstIserted = 0;
			
			for(String updateRequest : boostsUpdate.getUpdateRequests()) {
				bstUpdated += SqlMethods.update(updateRequest, dbConnection);
			}
			
			if(boostsUpdate.isNeedInsert())
				bstIserted = SqlMethods.insert(boostsUpdate.getInsertRequest(), dbConnection);
			
			sendHttpResponse(ctx, buildResponse("{udpated : "+bstUpdated+", inserted: "+bstIserted+"}", HttpResponseStatus.OK));
			
			break;
		case LEADBOARD:
			String playersTableName = game.getPrefix()+"players";
			String levelsTableName = game.getPrefix()+"levels";
			
			String sql = "SELECT p.facebookId, max(l.level) max_lvl FROM %s l " + 
						 "JOIN %s p ON p.playerId = l.playerId " + 
						 "GROUP BY p.facebookId " + 
						 "ORDER BY max_lvl DESC";
			
			List<Row> rows = SqlMethods.select(String.format(sql, levelsTableName, playersTableName), dbConnection);
			sendHttpResponse(ctx, buildResponse(Row.rowsToJson(rows), HttpResponseStatus.OK));
			
			break;
		case PLAYERPROGRESS:
			JSONObject filter = new JSONObject(inputContent);
			
			JSONArray fbIds = filter.has("f_ids")?filter.getJSONArray("f_ids"):null;
			int level = filter.has("level")?filter.getInt("level"):0;
			
			if(fbIds == null || fbIds.length() <= 0 || level <= 0) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Filter fail", HttpResponseStatus.BAD_REQUEST));
				break;
			}
			
			String playersTableName1 = game.getPrefix()+"players";
			String levelsTableName1 = game.getPrefix()+"levels";
			
			String sql1 = "SELECT p.facebookId, l.level, l.score, l.stars FROM %s l " + 
						  "JOIN %s p ON p.playerId = l.playerId " +
						  "WHERE l.level=? AND p.facebookId IN (";
			
			List<QueryTypedValue> queryValues = new ArrayList<>();
			queryValues.add(new QueryTypedValue(level));
			
			for(int i=0; i<fbIds.length(); i++) {
				queryValues.add(new QueryTypedValue(fbIds.getString(i)));
				sql1+="?" + ( i<fbIds.length()-1?",":"" );
			}
			
			sql1+=")";
			
			List<Row> rows1 = SqlMethods.select(String.format(sql1, levelsTableName1, playersTableName1), queryValues, dbConnection);
			sendHttpResponse(ctx, buildResponse(Row.rowsToJson(rows1), HttpResponseStatus.OK));
			break;
		case MAXPLAYERPROGRESS:
			JSONArray fbIds1 = new JSONArray(inputContent);
			
			String playersTableName2 = game.getPrefix()+"players";
			String levelsTableName2 = game.getPrefix()+"levels";
			
			String sql2 = "SELECT p.facebookId, t1.level, t1.score, t1.stars FROM %s p\r\n" + 
					"JOIN (SELECT lb.playerId, lb.level, lb.score, lb.stars FROM %s lb\r\n" + 
					"      JOIN ( SELECT la.playerId, max(la.level) max_lvl FROM %s la\r\n" + 
					"             GROUP BY la.playerId\r\n" + 
					"			 ORDER BY max_lvl DESC ) lc\r\n" + 
					"      ON lb.playerId = lc.playerId AND lb.level = lc.max_lvl) t1\r\n" + 
					"ON p.playerId = t1.playerId";
			
			List<QueryTypedValue> queryValues1 = new ArrayList<>();
			
			if(fbIds1.length() > 0) {
				sql2 += "\r\nWHERE p.facebookId IN (";
				for(int i=0; i<fbIds1.length(); i++) {
					queryValues1.add(new QueryTypedValue(fbIds1.getString(i)));
					sql2+="?" + ( i<fbIds1.length()-1?",":"" );
				}
				
				sql2+=")";
			}
			
			List<Row> rows2 = SqlMethods.select(String.format(sql2, playersTableName2, levelsTableName2, levelsTableName2), queryValues1, dbConnection);
			sendHttpResponse(ctx, buildResponse(Row.rowsToJson(rows2), HttpResponseStatus.OK));
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
		
		
	}

	private void handleAllowedRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Game game) throws SQLException, FileNotFoundException, ClassNotFoundException, IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
		
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
		String requestName = recognizeSpecialRequestName(fullHttpRequest.uri());
		String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
		List<Row> rows = DataBaseMethods.executeSpecialRequest(game.getId(), requestName, game.getPrefix(), new JSONArray(inputContent), dbConnection);
		
		sendHttpResponse(ctx, buildResponse(Row.rowsToJson(rows), HttpResponseStatus.OK));
	}
	
	/*private void handleSpecailRequestCreation(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, FileNotFoundException, ClassNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException {
		
		Game game = game(httpRequest);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
			return;
		}
		
		Authorization auth = new Authorization();
		
		if(!auth.authorization(httpRequest, game, dbConnection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
			return;
		}
		
		Map<String, String> contentParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!simpleValidation(new String[] {"request_name", "query_table", "fields_set"}, contentParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String requestName = contentParameters.get("request_name");
		String tableName = contentParameters.get("query_table");
		String fields = contentParameters.get("fields_set");
		
		String[] fieldsArray = fields.split(",");
		for(String field : fieldsArray) {
			field = field.trim();
		}*/
		
		/*List<AllowedUnconditionalRequest> allowedRequest = new ArrayList<>();
		allowedRequest.add(new AllowedUnconditionalRequest(game.getApiKey(), requestName, tableName, fieldsArray));
		addAllowedRequestsToFile(allowedRequest, game.getApiKey());*/
		/*if(DataBaseMethods.setSpecialRequest(game.getId(), requestName, tableName, fields, dbConnection) > 0) {
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Request \\\""+requestName+"\\\" has been set successfully", HttpResponseStatus.OK));
		} else {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Fail to create request", HttpResponseStatus.OK));
		}
	}*/

	private Game parseGame(FullHttpRequest httpRequest) throws SQLException {
		
		String authorizationString = httpRequest.headers().get("Authorization");
		
		if(authorizationString != null && authorizationString.length() > 0 && authorizationString.contains(":")) {
			String gameHash = authorizationString.substring(authorizationString.indexOf(":")+1);
			return DataBaseMethods.getGameByHash(gameHash, dbConnection);
		}

		String apiKey = httpRequest.headers().get("API_key");
		
		if(apiKey != null && apiKey.length() > 0) {
			return DataBaseMethods.getGameByKey(apiKey, dbConnection);
		}
		
		return null;
	}

	private void handlePlayerRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Game game) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
		Map<String, String> contentParameters = parseParameters(fullHttpRequest.content().toString(CharsetUtil.UTF_8));
		if(fullHttpRequest.uri().startsWith("/player/authorization")) {
			
			if(!simpleValidation(new String[] {"facebookId"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String facebookId = contentParameters.get("facebookId");
			Player player = DataBaseMethods.getPlayerByFacebookId(facebookId, game.getPrefix(), dbConnection);
			
			if(player != null) {
				sendHttpResponse(ctx, buildSimpleResponse("playerId", player.getPlayerId(), HttpResponseStatus.OK));
				return;
			}
			
			String playerId = DataBaseMethods.registrationPlayerByFacebookId(contentParameters.get("facebookId"), game.getPrefix(), dbConnection);
			if(playerId != null) {
				sendHttpResponse(ctx, buildSimpleResponse("playerId", playerId, HttpResponseStatus.OK));
			} else {
				sendValidationFailResponse(ctx);
			}
		}
	}

	private void handleTemplateRequest(ChannelHandlerContext ctx, HttpRequest httpRequest, Game game) throws SQLException {
		String responseContent = "";
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key", "table", "playerId"}, urlParameters)) {
			sendValidationFailResponse(ctx);
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
		case REGISTER_GAME:
			handleRegisterGame(ctx, httpRequest);
			break;
		/*case CREATE_SPEC_REQUEST:
			handleSpecailRequestCreation(ctx, httpRequest);
			break;*/
		case SPEC_REQUEST_LIST:
			handleSpecailRequestsList(ctx, httpRequest);
			break;
		case DELETE_GAME:
			handleDeleteGame(ctx, httpRequest);
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
	}
	
	private void handleSpecailRequestsList(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException {
		Game game = parseGame(httpRequest);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
			return;
		}
		
		Authorization auth = new Authorization();
		
		if(!auth.authorization(httpRequest, game, dbConnection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
			return;
		}
		
		List<SpecialRequest> specRequests = DataBaseMethods.readSpecialRequests(game.getId(), dbConnection);
		StringBuilder output = new StringBuilder();
		
		for (int i = 0; i < specRequests.size(); i++) {
			output.append(specRequests.get(i).getRequestName());
			if(i < specRequests.size()-1) {
				output.append(",");
			}
		}
		
		sendHttpResponse(ctx, buildSimpleResponse("special_requests", output.toString(), HttpResponseStatus.OK));
	}

	private void handleDeleteGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException {
		Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
		if(!simpleValidation(new String[] {"api_key"}, urlParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String apiKey = urlParameters.get("api_key");
		
		Game game = DataBaseMethods.deleteGame(apiKey, dbConnection);
		
		if(game == null) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while deleting the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		DataBaseMethods.deleteGameTables(game.getPrefix(), MATCH_3_TEMPLATE.getTableNames(), dbConnection);
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Deletion was successful", HttpResponseStatus.OK));
	}

	private void handleRegisterGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException {
		Map<String, String> bodyParameters = parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!simpleValidation(new String[] {"game_name", "email", "match3", "send_mail"}, bodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		boolean isMath3 = "Yes".equals(bodyParameters.get("match3"));
		
		String gameName = bodyParameters.get("game_name");
		String gameType = isMath3?"math3":"default";
		String email = bodyParameters.get("email");
		String apiKey = RandomKeyGenerator.nextString(24);
		String apiSecret = RandomKeyGenerator.nextString(45);
		
		if(DataBaseMethods.checkGameByKey(apiKey, dbConnection)) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Game alredy exists", HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		Owner owner = DataBaseMethods.getOwnerByEmail(email, dbConnection);
		
		if(owner == null) {
			int result = DataBaseMethods.regOwner(email, dbConnection);
			
			if(result <= 0) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Error occurred during registration", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			
			owner = DataBaseMethods.getOwnerByEmail(email, dbConnection);
		}
		
		dbConnection.setAutoCommit(false);
		String prefix = DataBaseMethods.generateTablePrefix(gameName, apiKey);
		int added = DataBaseMethods.insertGame(gameName, gameType, owner.getId(), apiKey, apiSecret, prefix, Authorization.generateHmacHash(apiSecret, apiKey), dbConnection);
		
		if(added < 1) {
			dbConnection.rollback();
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while adding the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		if("Yes".equals(bodyParameters.get("match3"))) {
			DataBaseMethods.createGameTables(MATCH_3_TEMPLATE, prefix, dbConnection); //throw exception if not successfully
		} else {
			DataBaseMethods.createGameTable(new TableTemplate("players", new Field[] {new Field(Types.VARCHAR, "playerId").setLength(17).setNull(false), new Field(Types.VARCHAR, "facebookId")}, "playerId"), prefix, dbConnection);
		}
		
		dbConnection.commit();
		dbConnection.setAutoCommit(true);
		
		//Game game = DataBaseMethods.getGameByKey(apiKey, connection);
		if("Yes".equals(bodyParameters.get("send_mail"))) {
			emailSender.asyncSend(email, "Your game registerd", "Game name: \""+gameName+"\" ApiKey: "+apiKey+" Api secret: "+apiSecret);
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Register successed", HttpResponseStatus.OK));
		} else {
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("game_name", gameName);
			response.put("api_key", apiKey);
			response.put("api_secret", apiSecret);
			sendHttpResponse(ctx, response(jsonObject(response), HttpResponseStatus.OK));
		}
	}

	/*private SqlRequest buildRequest(FullHttpRequest httpRequest/*RequestName requestName, String data*//*, String tableName) throws JSONException, SQLException {*/
	
		/*SqlRequest result = null;
		List<SqlExpression> whereData;
		RequestName requestName = recognizeRequestName(httpRequest.uri());
		switch (requestName) {
		case SELECT:
			whereData = SqlMethods.parseWhere(new JSONArray(httpRequest.content().toString(CharsetUtil.UTF_8)));
			result = new SqlSelect(tableName, whereData);
			break;
		case INSERT:*/
			
			//Map<String, String> urlParameters = parseUrlParameters(httpRequest.uri());
			
			/*if(urlParameters.containsKey("updateIfExists") && "true".equals(urlParameters.get("updateIfExists"))) {
				JSONObject jsonObject = new JSONObject(httpRequest.content().toString(CharsetUtil.UTF_8));
				List<SqlExpression> where = SqlMethods.parseWhere(jsonObject.getJSONArray("where_condition"));
				List<Row> row = SqlMethods.selectAll(tableName, where, connection);
				if(row.size() > 0) {
					new SqlUpdate(tableName, where, SqlMethods.parseWhere(jsonObject.getJSONArray("insert_row")));
				} else {
					List<SqlExpression> insertData = SqlMethods.parseCellDataRow(new JSONArray(httpRequest.content().toString(CharsetUtil.UTF_8)));
					result = new SqlInsert(tableName, insertData);
				}
			} else {*/
				//List<SqlExpression> insertData = SqlMethods.parseCellDataRow(new JSONObject(httpRequest.content().toString(CharsetUtil.UTF_8)).getJSONArray("insert_data"));
				/*List<SqlExpression> insertData = SqlMethods.parseCellDataRow(new JSONArray(httpRequest.content().toString(CharsetUtil.UTF_8)));	
				result = new SqlInsert(tableName, insertData);*/
			
			//}
			/*break;
		case UPDATE:
			JSONObject updateData = new JSONObject(httpRequest.content().toString(CharsetUtil.UTF_8));
			
			whereData = SqlMethods.parseWhere(updateData.getJSONArray("where"));
			List<SqlExpression> setData = SqlMethods.parseCellDataRow(updateData.getJSONArray("set"));
			
			result = new SqlUpdate(tableName, whereData, setData);
			break;
		default:
			return null;
		}
			
		return result;
		
	}*/
	
	private void handleApiRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, Game game) throws SQLException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
		String responseContent = "";
		
		RequestName requestName = recognizeRequestName(httpRequest.uri());
		String requestBody = httpRequest.content().toString(CharsetUtil.UTF_8);
		PlayerId playerId = new PlayerId("playerId", httpRequest.headers().get(Authorization.PLAYER_ID_HEADER));
		boolean objectQuery = Boolean.parseBoolean(httpRequest.headers().get("Test-Object-Query"));
		
		JSONObject jsonQuery = new JSONObject(requestBody);
		
		switch (requestName) {
		case SELECT:
			List<Row> rows = new ArrayList<>();
			Select select = new Select(jsonQuery);
			
			if(objectQuery) {
				rows = ApiMethods.select(select, playerId, game.getPrefix(), dbConnection);
			} else {
				rows = ApiMethods.select(jsonQuery, playerId, game.getPrefix(), dbConnection);
			}
			
			responseContent = Row.rowsToJson(rows);
				
			break;
		case INSERT:
			if(ApiMethods.insert(jsonQuery, playerId, game.getPrefix(), dbConnection) > 0) {
				responseContent = jsonObject("Success", "Insert completed successfully");
			} else {
				responseContent = jsonObject("Error", "An error occurred while inserting");
			}
			break;
		case UPDATE:
			if(ApiMethods.update(jsonQuery, playerId, game.getPrefix(), dbConnection) > 0) {
				responseContent = jsonObject("Success", "Update completed successfully");
			} else {
				responseContent = jsonObject("Error", "An error occurred while updating");
			}
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
	}
	
	/*private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus, Map<String, String> headers) {
		return buildResponse(jsonObject(status, message), httpStatus, headers);
	}*/
	
	private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus) {
		return buildResponse(jsonObject(status, message), httpStatus);
	}
	
	private FullHttpResponse response(String message, HttpResponseStatus httpStatus) {
		return buildResponse(message, httpStatus);
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
	
	
	
	private static GameTemplate match3Template() {
		
		List<TableTemplate> tblTemplates = new ArrayList<>();
		TableTemplate levelsTemplate = new TableTemplate("levels", new Field[] {  new Field(Types.INTEGER, "id").setNull(false).setAutoIncrement(true),
																				  new Field(Types.VARCHAR, "playerId").setNull(false),
				  																  new Field(Types.INTEGER, "level").setNull(false),
																				  new Field(Types.INTEGER, "score").setNull(false),
																				  new Field(Types.INTEGER, "stars").setNull(false)}, "id");
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));
		
		TableTemplate playersTemplate = new TableTemplate("players", new Field[] {  new Field(Types.VARCHAR, "playerId").setNull(false).setLength(17),
																					new Field(Types.VARCHAR, "facebookId").setNull(false)}, "playerId");
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"facebookId"}, true));
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", new Field[] {new Field(Types.INTEGER, "id").setNull(false).setAutoIncrement(true),
																			new Field(Types.VARCHAR, "playerId").setNull(false), 
				  																  new Field(Types.VARCHAR, "name").setNull(false), 
																				  new Field(Types.INTEGER, "count").setDefaultValue("0")}, "id");
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "name"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		
		return new GameTemplate("Match 3", tblTemplates);
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
	
	/*private Map<String, AllowedUnconditionalRequest> readAllowedRequestFile(String apiKey) throws FileNotFoundException, IOException, ClassNotFoundException {
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
	}*/
	
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
	
	private String jsonObject(String name, String value) {
		return "{\""+name+"\":\""+value+"\"}";
	}
	
	private String jsonObject(Map<String, Object> vars) {
		StringBuilder result = new StringBuilder("{");
		for(Map.Entry<String, Object> e : vars.entrySet()) {
			Object value = e.getValue();
			result.append("\"").append(e.getKey()).append("\"").append(":");

			if(value instanceof String) {
				result.append("\"").append(value).append("\",");
			} else {
				result.append(value).append(",");
			}
		}
		return result.substring(0, result.lastIndexOf(","))+"}";
	}
}
