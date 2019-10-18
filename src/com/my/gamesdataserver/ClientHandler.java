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
	private StringBuilder logContent = new StringBuilder();
	private HttpResponse httpResponse = new HttpResponse("1.1", 200, "");
	private String logPrefix = "";
	private Settings settings;
	private static final String[] MATH_3_TABLE_SET = {"ScoreLevel", "Level", "Boosts"};
	
	private enum RequestGroup {API, SYSTEM, CONTENT, BAD};
	
	public ClientHandler(GamesDbManager dbm, LogManager logManager, Settings settings) throws IOException {
		this.dbm = dbm;
		this.access = new Access();
		this.logManager = logManager;
		this.settings = settings;
		logPrefix = "system";
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
	    
	    HttpRequest httpRequest = new HttpRequest(inputString);
	    
	    logContent.append(getInputRequestLog(inputString, "nnn.nnn.nnn.nnn", false));
	    
		if(access.isDiscardRequest(httpRequest.getUrl()) || access.isWrongSymbols(httpRequest.getUrl()))  {
			logManager.log(logPrefix, logContent.append("Suppressed because not accessible").toString());
			return;
		}
		
		RequestGroup requestGroup = recognizeRequestGroup(httpRequest);
		
		try {
			
			switch (requestGroup) {
			case API:
				handleApiRequest(ctx, httpRequest);
				break;

			case SYSTEM:
				handleSystemRequest(ctx, httpRequest);
				break;
				
			case CONTENT:
				break;
				
			default:
				httpResponse.setContent(simpleJsonObject("Internal error", "Bad request group"));
				sendHttpResponse(ctx, httpResponse);
			}
		
		} catch (JSONException | SQLException | MessagingException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logManager.log(logPrefix, sw.toString());
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
			Map<String, String> contentParameters = httpRequest.getUrlParametrs();//httpRequest.parseContentWithParameters();
			
			if(!contentParameters.containsKey("email")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String inputEmail = contentParameters.get("email");
			
			if(!dbm.checkOwnerByEmail(inputEmail)) {
				int result = dbm.regOwner(inputEmail);
				if(result <= 0) {
					logManager.log(logPrefix, "registration fail");
				}
			}
			
			String newApiKey = RandomKeyGenerator.nextString(45);
			int added = dbm.writeNewApiKey(inputEmail, newApiKey);
			
			if(added > 0) {
				EmailSender.send(contentParameters.get("email"), "New API key generation", "Your API key generated: "+newApiKey);
				httpResponse.setContent("API key generated successfully");
				sendHttpResponse(ctx, httpResponse);
			} else {
				logManager.log(logPrefix, "Cannot write new api key");
				httpResponse.setContent("Internal error");
				sendHttpResponse(ctx, httpResponse);
			}
		} else if(httpRequest.getUrl().startsWith("/system/register_game")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			if(!contentParameters.containsKey("api_key") || 
					!contentParameters.containsKey("game_name") ||
					!contentParameters.containsKey("game_package")) {
				httpResponse.setContent("Parameters validation failed");
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			String apiKey = contentParameters.get("api_key");
			String gameName = contentParameters.get("game_name");
			String gameJavaPackage = contentParameters.get("game_package");
			
			if(!dbm.checkGamesKeys(apiKey)) {
				int ownerId = dbm.readOwnerIdFromApiKeys(apiKey);
				
				if(ownerId < 1) {
					logManager.log(logPrefix, "Cannot pop owner id");
					httpResponse.setContent("Internal error");
					sendHttpResponse(ctx, httpResponse);
					return;
				}
				
				int added = dbm.initGameSet(gameName, gameJavaPackage, ownerId, apiKey, MATH_3_TABLE_SET, apiKey.substring(0, 8));
				
				if(added < 1) {
					logManager.log(logPrefix, "Cannot write to games");
					httpResponse.setContent("Internal error");
					sendHttpResponse(ctx, httpResponse);
					return;
				}
				
				dbm.createMatch3TableSet(apiKey.substring(0, 8)); //throw exception if not successfully
				
				dbm.removeApiKey(apiKey);
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game "+apiKey+" registered");
				
			} else if(contentParameters.containsKey("update") && "true".equals(contentParameters.get("update")) 
					  && contentParameters.containsKey("new_game_name") && contentParameters.containsKey("new_game_package")) {
				
				dbm.updateGame(contentParameters.get("new_game_name"), contentParameters.get("new_game_package"));
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game data "+apiKey+" updated");
			
			} else {
				httpResponse.setContent("Game alredy exists");
				sendHttpResponse(ctx, httpResponse);
			}
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

	/*public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String httpRequest = readRequest(in);
			String url = parseUrl(httpRequest);
			String urlPath = parseUrlPath(url);
			
			if(httpRequestFilter.filterForbiddens(urlPath))  {
				System.out.println("Request parsing failed or filtered.");
				return;
			}
			
			printHttpRequest(httpRequest, ipAddress, false);
			
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			
			Request request = new Request(url);
			
			if(request.validateParametersWithSchema()) {
				commandRequestProcessor(request, out);
			} else if(access.isAllowedPath(urlPath) && url.contains("key="+Access.contentAccessKey)) {
				contentRequestProcessor(urlPath, out);
			} else {
				sendHttpResponse(out, simpleJsonObject("Request error", "Bad request"));
				return;
			}
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
			sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				socket.close();
			}
			catch (IOException e) {
				System.err.println("Socket not closed");
				System.err.println(e.getMessage());
			}
			System.out.println(ipAddress+" Socket closed.");
		}
	}*/
    
	/*private String readRequest(ByteBuf in) {
		StringBuilder result = new StringBuilder(in.capacity());
		
		while (in.isReadable()) {
			result.append(in.readChar());
	    }
		
		return result.toString();
	}*/
	
	/*private void contentRequestProcessor(String urlPath, ChannelHandlerContext ctx) throws IOException {
		String workPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getCanonicalPath().replaceAll("%20", " ");
		String content = readFile(workPath+urlPath.replace('/', File.separatorChar), StandardCharsets.UTF_8);
		sendHttpResponse(ctx, content);
	}*/
	
	/*private void commandRequestProcessor(AbstractSqlRequest.Type command, AbstractSqlRequest request, ChannelHandlerContext ctx) throws IOException, JSONException, SQLException {
		
		Map<String, String> getParameters = request.getParameters();*/
		//AbstractRequest.Type command = request.getCommand();
		
		/*switch (command) {
		case READ_SAVE:
			readSave(getParameters, ctx);
			
			break;
		case REGISTER_PLAYER:
			registerPlayer(getParameters, ctx);
			
			break;
		case ADD_GAME:
			addGame(getParameters, ctx);
			
			break;
		case UPDATE_SAVE:
			updateSave(getParameters, ctx);
			
			break;
		case REGISTER_OWNER:
			registerOwner(getParameters, ctx);
			
			break;
		case UPDATE_BOOST:
			updateBoost(getParameters, ctx);
			
			break;
		case MONITOR_DATA:
			monitorData(ctx);
			break;
		case INSERT_INTO_TABLE:
			int changed = dbm.insertToTable(request.getTableName(), ((SqlInsert)request).getData());
			sendHttpResponse(ctx, simpleJsonObject("result", ""+changed));
			break;
		case UPDATE_TABLE:
			int changed2 = dbm.updateTable(request.getTableName(), ((SqlUpdate)request).getSet(), ((SqlUpdate)request).getWhere());
			sendHttpResponse(ctx, simpleJsonObject("result", ""+changed2));
			break;
		}
    }*/
	
	/*private void readSave(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException, JSONException {
		GameEntity game = dbm.selectGameByApiKey(getParameters.get("game_api_key"));
		
		if(game == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Game not found"));
			return;
		}
		
		PlayerEntity player = dbm.selectPlayer(getParameters.get("player_id"), game.getId());
		
		if(player == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Player not found"));
			return;
		}
		
		SaveEntity save = dbm.selectSave(game.getId(), player.getId());
		
		if(save == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Failed to read save data"));
			return;
		}
		
		sendHttpResponse(ctx, "{\"save_data\" : \""+save.getSaveData()+"\", \"boost_data\" : \""+save.getBoostData()+"\" }");
	}
	
	private void registerPlayer(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException {
		GameEntity game = dbm.selectGameByApiKey(getParameters.get("game_api_key"));
		
		if(game == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Game not found"));
			return;
		}
		
		PlayerEntity player = dbm.selectPlayer(getParameters.get("player_id"), game.getId());
		
		if(player != null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "You alredy registered in this game"));
			return;
		}
		
		int rowsAdded = dbm.insertPlayer(getParameters.get("player_name"), getParameters.get("player_id"), game.getId());
		
		if(rowsAdded > 0) {
			sendHttpResponse(ctx, simpleJsonObject("Success", "Player added"));
		} else {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Player not added"));
		}
	}
	
	private void addGame(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException {
		GameOwnerEntity owner = dbm.selectOwnerByName(getParameters.get("owner_name"));
		
		if(owner == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Owner not found"));
			return;
		}
		
		RandomKeyGenerator keyGen = new RandomKeyGenerator();
		String key = keyGen.nextString(45);
		int rowsAdded1 = dbm.insertGame(owner.getId(), getParameters.get("name"), key);
		
		if(rowsAdded1 > 0) {
			sendHttpResponse(ctx, simpleJsonObject("Your game API key", key));
		} else {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Game not added"));
		}
	}
	
	private void updateSave(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException {
		GameEntity game = dbm.selectGameByApiKey(getParameters.get("game_api_key"));
		
		if(game == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Game not found"));
			return;
		}
		
		PlayerEntity player = dbm.selectPlayer(getParameters.get("player_id"), game.getId());
		
		if(player == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Player not found"));
			return;
		}
		
		SaveEntity save = dbm.selectSave(game.getId(), player.getId());
		int rows = 0;
		if(save == null) {
			rows = dbm.insertSave(game.getId(), player.getId(), getParameters.get("save_data"), "{}");
		} else {
			rows = dbm.updateSave(save.getId(), getParameters.get("save_data"));
		}
		
		if(rows > 0) {
			sendHttpResponse(ctx, simpleJsonObject("Sucess", "Game save updated"));
			return;
		}
		
		sendHttpResponse(ctx, simpleJsonObject("Fail", "Failed to update or insert save"));
	}
	
	private void registerOwner(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException {
		int newRowsCount = dbm.insertOwner(getParameters.get("name"));
		if(newRowsCount > 0) {
			sendHttpResponse(ctx, simpleJsonObject("Success", "New owner added"));
		} else {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "New owner not added"));
		}
	}
	
	private void updateBoost(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException {
		GameEntity game = dbm.selectGameByApiKey(getParameters.get("game_api_key"));
		
		if(game == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Game not found"));
			return;
		}
		
		PlayerEntity player = dbm.selectPlayer(getParameters.get("player_id"), game.getId());
		
		if(player == null) {
			sendHttpResponse(ctx, simpleJsonObject("Fail", "Player not found"));
			return;
		}
		
		SaveEntity save = dbm.selectSave(game.getId(), player.getId());
		int rows = 0;
		if(save == null) {
			rows = dbm.insertSave(game.getId(), player.getId(), "[0]", getParameters.get("boost_data"));
		} else {
			rows = dbm.updateBoost(save.getId(), getParameters.get("boost_data"));
		}
		
		if(rows > 0) {
			sendHttpResponse(ctx, simpleJsonObject("Sucess", "Boost data updated"));
			return;
		}
		
		sendHttpResponse(ctx, simpleJsonObject("Fail", "Failed to update boost data"));
	}
	
	private void monitorData(ChannelHandlerContext ctx) throws SQLException {
		List<GameOwnerEntity> gameOwners = dbm.selectOwners();
		List<GameEntity> games = dbm.selectGames();
		List<PlayerEntity> players = dbm.selectPlayers();
		List<SaveEntity> saves = dbm.selectSaves();
		
		sendHttpResponse(ctx, prepareJsonForMonitor(gameOwners, games, players, saves));
	}*/
	
	/*private String prepareJsonForMonitor(List<GameOwnerEntity> gameOwners, List<GameEntity> games, List<PlayerEntity> players, List<SaveEntity> saves) {
		
		StringBuilder result = new StringBuilder();
		result.append("{\"game_owners\":[");
		
		for(int i=0;i<gameOwners.size();i++) {
			result.append(gameOwners.get(i).toJson());
			if(i<gameOwners.size()-1) {
				result.append(",");
			}
		}
		
		result.append("],\"games\":[");
		
		for(int i=0;i<games.size();i++) {
			result.append(games.get(i).toJson());
			if(i<games.size()-1) {
				result.append(",");
			}
		}
		
		result.append("],\"players\":[");
		
		for(int i=0;i<players.size();i++) {
			result.append(players.get(i).toJson());
			if(i<players.size()-1) {
				result.append(",");
			}
		}
		
		result.append("],\"saves\":[");
		
		for(int i=0;i<saves.size();i++) {
			result.append(saves.get(i).toJson());
			if(i<saves.size()-1) {
				result.append(",");
			}
		}
		
		result.append("]}");
		
		return result.toString();
	}*/
	
	String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
    
	void sendHttpResponse(ChannelHandlerContext ctx, HttpResponse httpResponse) {
		ctx.writeAndFlush(Unpooled.copiedBuffer(httpResponse.toString(), CharsetUtil.UTF_8));
		logManager.log(logPrefix, logContent.append(getHttpResponseLog(httpResponse, "nnn.nnn.nnn.nnn", true)).toString());
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
	
	private String getInputRequestLog(String httpRequest, String ipAddress, boolean isLogHeader) {
		StringBuilder result = new StringBuilder();
		result.append("\nRequest from ");
		result.append(ipAddress);
		result.append("\n");
		if(isLogHeader) {
			result.append(httpRequest);
		} else {
			result.append(httpRequest.substring(0, httpRequest.indexOf("\n")));
		}
		
		return result.toString();
	}
	
	private String getHttpResponseLog(HttpResponse httpResponse, String ipAddress, boolean cutContent) {
		StringBuilder result = new StringBuilder();
		result.append("\nHTTP response to: ").append(ipAddress).append("\n");
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
