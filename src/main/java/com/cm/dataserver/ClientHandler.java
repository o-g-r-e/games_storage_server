package com.cm.dataserver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.basedbclasses.Field;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TableIndex;
import com.cm.dataserver.basedbclasses.TableTemplate;
import com.cm.dataserver.basedbclasses.queryclasses.Select;
import com.cm.dataserver.dbengineclasses.ApiMethods;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.GameTemplate;
import com.cm.dataserver.dbengineclasses.Owner;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.dbengineclasses.SpecialRequest;
import com.cm.dataserver.helpers.EmailSender;
import com.cm.dataserver.helpers.LogManager;
import com.cm.dataserver.helpers.RandomKeyGenerator;
import com.cm.dataserver.template1classes.BoostsUpdate;
import com.cm.dataserver.template1classes.LevelsUpdate;
import com.cm.dataserver.template1classes.LifeRequest;
import com.cm.dataserver.template1classes.Player;
import com.cm.dataserver.template1classes.PlayerMessage;
import com.cm.dataserver.template1classes.GameMethods;

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
	private Connection dbConnection;
	//private GamesDbEngine dbManager;
	private LogManager logManager;
	private String errorLogFilePrefix = "error";
	private static final GameTemplate MATCH_3_TEMPLATE = GameTemplate.match3Template();
	//private static Map<String, String> defaultResponseHeaders;
	private final String defaultPlayerIdFieldName = "playerId";
	
	private EmailSender emailSender;
	
	static {
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
	
	@Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		ServerURI.RequestGroup requestGroup = ServerURI.requestGroup(fullHttpRequest.uri());

		try {
			dbConnection = dbConnectionPool.getConnection();
			
			Game game = parseGame(fullHttpRequest);
			
			if(ServerURI.isGameDependsRequest(requestGroup) && game == null) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "Game not found", HttpResponseStatus.OK));
				return;
			}
			
			Authorization auth = new Authorization();
			
			if(requestGroup == ServerURI.RequestGroup.PLAYER_REQUEST && !auth.checkAuthorizationHeader(fullHttpRequest)) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", auth.getStatusMessage(), HttpResponseStatus.OK));
				return;
			}
			
			if(requestGroup != ServerURI.RequestGroup.BASE && requestGroup != ServerURI.RequestGroup.PLAYER_REQUEST && !auth.authorization(fullHttpRequest, game, dbConnection)) {
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
				
			case ALLOWED_REQUEST:
				handleAllowedRequest(ctx, fullHttpRequest, game);
				break;
				
			case MESSAGE:
				handlePlayerMessage(ctx, fullHttpRequest, game);
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
		
		ServerURI.RequestName requestName = ServerURI.parseRequestName(fullHttpRequest.uri());
		
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
		case CREATE_LIFE_REQUEST:
			handleCreateLifeRequest(ctx, fullHttpRequest, game.getPrefix(), LifeRequestCreationType.NORMAL, playerId.getValue());
			break;
		case SEND_LIFE:
			
			//
			// This case create 'confirmed' life request, that to be able to send life directly to player, without opened request creation
			//
			
			handleCreateLifeRequest(ctx, fullHttpRequest, game.getPrefix(), LifeRequestCreationType.SEND_LIFE, playerId.getValue());
			break;
		case CONFIRM_LIFE_REQUEST:
			handleLifeRequestUpdateStatus(ctx, fullHttpRequest, game.getPrefix(), "confirm");
			break;
		case DENY_LIFE_REQUEST:
			handleLifeRequestUpdateStatus(ctx, fullHttpRequest, game.getPrefix(), "deny");
			break;
		case ACCEPT_LIFE:
			handleLifeRequestUpdateStatus(ctx, fullHttpRequest, game.getPrefix(), "deny");
			break;
		case REFUSE_LIFE:
			handleLifeRequestUpdateStatus(ctx, fullHttpRequest, game.getPrefix(), "deny");
			break;
		case LIFE_REQUESTS:
			JSONObject lifeRequests = GameMethods.getLifeRequests(game.getPrefix(), playerId.getValue(), dbConnection);
			sendHttpResponse(ctx, buildResponse(lifeRequests.toString(), HttpResponseStatus.OK));
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
	}
	
	enum LifeRequestCreationType { NORMAL, SEND_LIFE }
	
	private void handleCreateLifeRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String gamePrefix, LifeRequestCreationType status, String playerId) throws JSONException, SQLException {
		String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
		String lifeRequestStatus = "open";
		
		if(status == LifeRequestCreationType.SEND_LIFE) {
			lifeRequestStatus = "confirmed";
		}
		
		JSONArray inputArray = new JSONArray(inputContent);
		
		List<LifeRequest> lifeRequestList = new ArrayList<>();
		
		for (int i = 0; i < inputArray.length(); i++) {
			String lifeSenderFBid = inputArray.getString(i);
			
			if(!StringDataHelper.validateFacebookId(lifeSenderFBid)) {
				continue;
			}
			
			Player lifeSender = DataBaseMethods.getPlayerByFacebookId(lifeSenderFBid, gamePrefix, dbConnection);
			
			if(lifeSender == null) {
				continue;
			}
			
			if(status == LifeRequestCreationType.NORMAL) {
				lifeRequestList.add(new LifeRequest(generateUuidWithoutDash(), lifeSender.getPlayerId(), playerId, lifeRequestStatus));
			} else if(status == LifeRequestCreationType.SEND_LIFE) {
				lifeRequestList.add(new LifeRequest(generateUuidWithoutDash(), playerId, lifeSender.getPlayerId(), lifeRequestStatus));
			}
		}
		
		int created = GameMethods.createLifeRequests(gamePrefix, lifeRequestList, dbConnection);
		
		//
		// Not need check count of created requests, because there may be repeated requests for the same player that will not be created
		//
		
		/*if(created != lifeRequestList.size()) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while life request creation", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}*/
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Life reuqests created successfully", HttpResponseStatus.OK));
	}
	
	private void handleLifeRequestUpdateStatus(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String gamePrefix, String status) throws SQLException, JSONException {
		String inputContent = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
		JSONArray lifeRequestsIdsArray = new JSONArray(inputContent);
		List<String> lifeRequestIdList = new ArrayList<>();
		
		for (int i = 0; i < lifeRequestsIdsArray.length(); i++) {
			String lifeRequestid = lifeRequestsIdsArray.getString(i);
			
			if(!StringDataHelper.validateUUID(lifeRequestid)) {
				continue;
			}
			lifeRequestIdList.add(lifeRequestid);
		}
		
		int updated = 0;
		
		if(status == "confirm") {
			updated = GameMethods.confirmLifeRequests(gamePrefix, lifeRequestIdList, dbConnection);
		} else if(status == "deny") {
			updated = GameMethods.denyLifeRequests(gamePrefix, lifeRequestIdList, dbConnection);
		}
		
		if(updated < 1) {
			sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while life request status upadte", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		sendHttpResponse(ctx, buildSimpleResponse("Success", "Life reuqest statuss updated successfully", HttpResponseStatus.OK));
	}
	
	private String generateUuidWithoutDash() {
		return UUID.randomUUID().toString().replace("-", "");
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
		String requestName = StringDataHelper.parseSpecialRequestName(fullHttpRequest.uri());
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
		Map<String, String> contentParameters = StringDataHelper.parseParameters(fullHttpRequest.content().toString(CharsetUtil.UTF_8));
		if(fullHttpRequest.uri().startsWith("/player/authorization")) {
			
			if(!StringDataHelper.simpleValidation(new String[] {"facebookId"}, contentParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			String facebookId = contentParameters.get("facebookId");
			
			if(!StringDataHelper.validateFacebookId(facebookId)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
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

	private void handleSystemRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, MessagingException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, IOException, ClassNotFoundException {
		
		ServerURI.RequestName requestName = ServerURI.parseRequestName(httpRequest.uri());
		
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
		Map<String, String> urlParameters = StringDataHelper.parseUrlParameters(httpRequest.uri());
		if(!StringDataHelper.simpleValidation(new String[] {"api_key"}, urlParameters)) {
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
	
	private void handlePlayerMessage(ChannelHandlerContext ctx, FullHttpRequest httpRequest, Game game) throws SQLException {
		
		ServerURI.RequestName requestName = ServerURI.parseRequestName(httpRequest.uri());
		PlayerId playerId = new PlayerId("playerId", httpRequest.headers().get(Authorization.PLAYER_ID_HEADER));
		switch (requestName) {
		case SEND_MESSAGE:
			Map<String, String> bodyParameters = StringDataHelper.parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
			
			if(!StringDataHelper.simpleValidation(new String[] {"type", "recipient_facebook_id", "message"}, bodyParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			Player messageRecipient = DataBaseMethods.getPlayerByFacebookId(bodyParameters.get("recipient_facebook_id"), game.getPrefix(), dbConnection);
			
			int added = GameMethods.insertMessage(game.getPrefix(), bodyParameters.get("type"), playerId.getValue(), messageRecipient.getPlayerId(), bodyParameters.get("message"), dbConnection);
			
			if(added < 1) {
				dbConnection.rollback();
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while adding message", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Message sent successfully", HttpResponseStatus.OK));
			break;
			
		case FETCH_ALL_MESSAGES:
			List<Row> playerMessages = GameMethods.getPlayerMessages(game.getPrefix(), playerId.getValue(), dbConnection);
			sendHttpResponse(ctx, buildResponse(Row.rowsToJson(playerMessages), HttpResponseStatus.OK));
			break;
			
		case DELETE_MESSAGE:
			Map<String, String> mDeletionBodyParameters = StringDataHelper.parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
			
			if(!StringDataHelper.simpleValidation(new String[] {"id"}, mDeletionBodyParameters)) {
				sendValidationFailResponse(ctx);
				return;
			}
			
			int deleted = GameMethods.deleteMessage(game.getPrefix(), playerId.getValue(), mDeletionBodyParameters.get("id"), dbConnection);
			
			if(deleted < 1) {
				sendHttpResponse(ctx, buildSimpleResponse("Error", "An error occurred while deletion message", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			sendHttpResponse(ctx, buildSimpleResponse("Success", "Message deleted successfully", HttpResponseStatus.OK));
			break;
		}
		
	}
	
	

	private void handleRegisterGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException {
		Map<String, String> bodyParameters = StringDataHelper.parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!StringDataHelper.simpleValidation(new String[] {"game_name", "email", "match3", "send_mail"}, bodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		
		String m3 = bodyParameters.get("match3");
		String gameName = bodyParameters.get("game_name");
		String email = bodyParameters.get("email");
		
		if(!StringDataHelper.validateGameCreationParameters(gameName, email, m3)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		boolean isMath3 = "Yes".equals(m3);
		String gameType = isMath3?"math3":"default";
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
			DataBaseMethods.createGameTable(new TableTemplate("players", new Field[] {new Field(Types.VARCHAR, "playerId").setLength(17).defNull(false), new Field(Types.VARCHAR, "facebookId")}, "playerId"), prefix, dbConnection);
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
			sendHttpResponse(ctx, response(StringDataHelper.jsonObject(response), HttpResponseStatus.OK));
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
		
		ServerURI.RequestName requestName = ServerURI.parseRequestName(httpRequest.uri());
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
				responseContent = StringDataHelper.jsonObject("Success", "Insert completed successfully");
			} else {
				responseContent = StringDataHelper.jsonObject("Error", "An error occurred while inserting");
			}
			break;
		case UPDATE:
			if(ApiMethods.update(jsonQuery, playerId, game.getPrefix(), dbConnection) > 0) {
				responseContent = StringDataHelper.jsonObject("Success", "Update completed successfully");
			} else {
				responseContent = StringDataHelper.jsonObject("Error", "An error occurred while updating");
			}
			break;
		default:
			sendHttpResponse(ctx, buildSimpleResponse("Error", "Bad command", HttpResponseStatus.BAD_REQUEST));
			break;
		}
		
		sendHttpResponse(ctx, buildResponse(responseContent, HttpResponseStatus.OK));
	}
	
	private FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus) {
		return buildResponse(StringDataHelper.jsonObject(status, message), httpStatus);
	}
	
	private FullHttpResponse response(String message, HttpResponseStatus httpStatus) {
		return buildResponse(message, httpStatus);
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
}
