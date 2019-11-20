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

import javax.mail.MessagingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.ColData;
import com.my.gamesdataserver.basedbclasses.DataBaseInterface;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlRequest;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.basedbclasses.TableIndex;
import com.my.gamesdataserver.basedbclasses.TableTemplate;
import com.my.gamesdataserver.dbengineclasses.ApiKey;
import com.my.gamesdataserver.dbengineclasses.Game;
import com.my.gamesdataserver.dbengineclasses.GameTemplate;
import com.my.gamesdataserver.dbengineclasses.GamesDbEngine;
import com.my.gamesdataserver.dbengineclasses.Owner;
import com.my.gamesdataserver.deprecated.HttpResponse;
import com.my.gamesdataserver.helpers.EmailSender;
import com.my.gamesdataserver.helpers.LogManager;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;
import com.my.gamesdataserver.template1classes.Template1DbEngine;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class RequestHandler extends SimpleChannelInboundHandler<Object> {
	//private DataBaseInterface dbInterface;
		private GamesDbEngine dbManager;
		private Template1DbEngine template1DbManager;
		private LogManager logManager;
		//private Access access = new Access();
		//private StringBuilder errorLogMessage = new StringBuilder();
		private HttpRequest request;
		private StringBuilder rawRequest = new StringBuilder();
		//private HttpResponse httpResponse = new HttpResponse("1.1", 200, "");
		private static Map<String, String> defaultResponseHeaders = new HashMap<>();
		private String errorLogFilePrefix = "error";
		private String responseContent;
		//private static final GameTemplate gameTemplate = createGameTemplate1();
		
		private enum RequestGroup {BASE, API, TEMPLATE_API, BAD};
		
		static {
			defaultResponseHeaders.put("Access-Control-Allow-Origin", "*");
			defaultResponseHeaders.put("Content-type", "application/json");
			/*defaultResponseHeaders.put("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
			defaultResponseHeaders.put("Access-Control-Max-Age", "1000");
			defaultResponseHeaders.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");*/
		}
		
		public RequestHandler(DataBaseInterface dbInterface, LogManager logManager) throws IOException {
			//this.dbInterface = dbInterface;
			this.dbManager = new GamesDbEngine(dbInterface);
			this.template1DbManager = new Template1DbEngine(dbInterface);
			this.logManager = logManager;
		}
		
		private RequestGroup recognizeRequestGroup(HttpRequest httpRequest) {
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
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}
	
	/*@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}*/
		
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if (msg instanceof HttpRequest) {
			request = (HttpRequest) msg;
			rawRequest.setLength(0);
			rawRequest.append(request.method().name()).append(" ");
			rawRequest.append(request.uri()).append(" ").append("HTTP/");
			rawRequest.append(request.protocolVersion().protocolName()).append("\r\n");
			HttpHeaders headers = request.headers();
			if (!headers.isEmpty()) {
		        for (Map.Entry<String, String> h: headers) {
		        	rawRequest.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
		        }
		        rawRequest.append("\r\n");
		    }
			//System.out.println("HEAD");
			if (HttpUtil.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}
		}
		
		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			//System.out.println("CONTENT");
			if (msg instanceof LastHttpContent) {
				LastHttpContent trailer = (LastHttpContent) msg;
				
				RequestGroup requestGroup = recognizeRequestGroup(request);
				
				try {
					
					switch (requestGroup) {

					case BASE:
						handleSystemRequest(ctx, request);
						break;
						
					case API:
						handleApiRequest(ctx, request);
						break;
						
					case TEMPLATE_API:
						handleTemplateRequest(ctx, request);
						break;
						
					case BAD:
						//httpResponse.setContent(simpleJsonObject("Error", "Bad request group"));
						responseContent = simpleJsonObject("Error", "Bad request group");
						logManager.log(errorLogFilePrefix, rawRequest, "ERROR: Bad request group".toUpperCase(), httpResponse.toString());
						sendHttpResponse(ctx, httpResponse);
						break;
					}
					
					/*if(errorLogMessage.length() > 0) {
						logManager.log(errorLogFilePrefix, inputString+"\n\n"+errorLogMessage+"\n\n"+httpResponse.toString());
					}*/
				
				} catch (JSONException | SQLException | MessagingException e) {
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
					httpResponse.setContent(simpleJsonObject("Error", "An error occurred during processing"));
					logManager.log(errorLogFilePrefix, inputString, sw.toString(), httpResponse.toString());
					sendHttpResponse(ctx, httpResponse);
				}
				
				if (!writeResponse(trailer, ctx)) {
					// If keep-alive is off, close the connection once the content is fully written.
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				}
			}
		}
	}

	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
	    ctx.write(response);
	}
	
	private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
	    // Decide whether to close the connection or not.
	    boolean keepAlive = HttpUtil.isKeepAlive(request);
	    // Build the response object.
	    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, 
	    		currentObj.decoderResult().isSuccess()? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST,
	            Unpooled.copiedBuffer("response content", CharsetUtil.UTF_8));
	
	    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
	
	    if (keepAlive) {
	        // Add 'Content-Length' header only for a keep-alive connection.
	        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
	        // Add keep alive header as per:
	        // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
	        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
	    }
	
	    // Encode the cookie.
	    //String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
	    //if (cookieString != null) {
	        //Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
	        //if (!cookies.isEmpty()) {
	            // Reset the cookies if necessary.
	            //for (Cookie cookie: cookies) {
	                //response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
	            //}
	        //}
	    //} else {
	        // Browser sent no cookie.  Add some.
	        //response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key1", "value1"));
	        //response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key2", "value2"));
	    //}
	
	    // Write the response.
	    ctx.write(response);
	
	    return keepAlive;
	}
	
	private void handleTemplateRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException {
		String responseContent = "";
		if(!simpleValidation(new String[] {"api_key", "table", "playerId"}, httpRequest.getUrlParametrs())) {
			sendValidationFailResponse(ctx, httpResponse);
			return;
		}
		
		String apiKey = httpRequest.getUrlParametrs().get("api_key");
		
		Game game = dbManager.getGameByKey(apiKey);
		
		if(game == null) {
			httpResponse.setContent(simpleJsonObject("Error", "Game not found"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
		
		template1DbManager.setTablePrefix(game.getPrefix());
		
		String playerId = httpRequest.getUrlParametrs().get("playerId");
		
		Player player = template1DbManager.getPlayer(playerId);
		
		if(player == null) {
			httpResponse.setContent(simpleJsonObject("Error", "Player not found"));
			sendHttpResponse(ctx, httpResponse);
			return;
		}
		
		if(httpRequest.getUrl().startsWith("/api/template1/completeLevel")) {
			
		}
		
		httpResponse.setContent(responseContent);
		sendHttpResponse(ctx, httpResponse);
	}

	private void handleSystemRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) throws SQLException, MessagingException {
		String responseContent = "";
		if(httpRequest.getUrl().startsWith("/system/generate_api_key")) {
			
			//DatabaseEngine dbManager = new DatabaseEngine(dbInterface);
			if(!simpleValidation(new String[] {"email"}, httpRequest.getUrlParametrs())) {
				sendValidationFailResponse(ctx, httpResponse);
				return;
			}
			
			String inputEmail = httpRequest.getUrlParametrs().get("email");
			Owner owner = dbManager.getOwnerByEmail(inputEmail);
			if(owner == null) {
				int result = dbManager.regOwner(inputEmail);
				
				if(result <= 0) {
					//errorLogMessage.append("Error during inserting owner.");
					httpResponse.setContent(simpleJsonObject("Error", "Error occurred during registration"));
					sendHttpResponse(ctx, httpResponse);
					return;
				}
				
				owner = dbManager.getOwnerByEmail(inputEmail);
			}
			
			String newApiKey = RandomKeyGenerator.nextString(45);
			int added = dbManager.writeNewApiKey(owner.getId(), newApiKey);
			
			if(added > 0) {
				EmailSender.send(inputEmail, "New API key generation", "Your API key generated: "+newApiKey);
				responseContent = simpleJsonObject("Success", "API key generated successfully");
			} else {
				//errorLogMessage.append("Cannot write new api key.");
				responseContent = simpleJsonObject("Error", "An error occurred while creating the key");
			}
			httpResponse.setContent(responseContent);
			sendHttpResponse(ctx, httpResponse);
		} else if(httpRequest.getUrl().startsWith("/system/register_game")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"api_key", "game_name", "game_package", "game_type"}, contentParameters)) {
				sendValidationFailResponse(ctx, httpResponse);
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
			
			ApiKey apiKeyEntity = dbManager.getApiKey(apiKey);
			
			if(apiKeyEntity == null) {
				//errorLogMessage.append("Cannot find game api key.");
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while searching for the key"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbManager.enableTransactions();
			String prefix = GamesDbEngine.generateTablePrefix(gameName, apiKey);
			int added = dbManager.insertGame(gameName, gameJavaPackage, apiKeyEntity.getOwnerId(), apiKey, gameType, prefix);
			
			if(added < 1) {
				dbManager.rollback();
				//errorLogMessage.append("Cannot write game.");
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while adding the game"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbManager.removeApiKey(apiKey);
			
			dbManager.commit();
			dbManager.disableTransactions();
			
			dbManager.createGameTables(gameTemplate, prefix); //throw exception if not successfully
			String email = dbManager.getOwnerEmailById(apiKeyEntity.getOwnerId());
			EmailSender.send(email, "Your game registerd", "Your game \""+gameName+"\" registered with key "+apiKey);
			httpResponse.setContent(simpleJsonObject("Success", "Register successed"));
			sendHttpResponse(ctx, httpResponse);
			
		} else if(httpRequest.getUrl().startsWith("/system/udpate_game_data")) {
			Map<String, String> contentParameters = httpRequest.parseContentWithParameters();
			
			if(!simpleValidation(new String[] {"api_key"}, contentParameters)) {
				sendValidationFailResponse(ctx, httpResponse);
				return;
			}
			
			int updated = dbManager.updateGame(contentParameters.get("new_game_name"), contentParameters.get("new_game_package"));
			// update tables names ???
			if(updated > 0) {
				EmailSender.send(contentParameters.get("email"), "Your game registerd", "Your game data "+contentParameters.get("api_key")+" updated");
			}
			
		} else if(httpRequest.getUrl().startsWith("/system/delete_game")) {
			if(!simpleValidation(new String[] {"api_key"}, httpRequest.getUrlParametrs())) {
				sendValidationFailResponse(ctx, httpResponse);
				return;
			}
			
			String apiKey = httpRequest.getUrlParametrs().get("api_key");
			
			Game game = dbManager.deleteGame(apiKey);
			
			if(game == null) {
				httpResponse.setContent(simpleJsonObject("Error", "An error occurred while deleting the game"));
				sendHttpResponse(ctx, httpResponse);
				return;
			}
			
			dbManager.deleteGameTables(game.getPrefix(), gameTemplate.getTableNames());
			
			httpResponse.setContent(simpleJsonObject("Success", "Deletion was successful"));
			sendHttpResponse(ctx, httpResponse);
		} else {
			httpResponse.setContent(simpleJsonObject("Error", "Bad command"));
			sendHttpResponse(ctx, httpResponse);
		}
	}
	
	private SqlRequest parseRequest(HttpRequest httpRequest, String tableNamePrefix) throws JSONException, SQLException {
		SqlRequest result = null;
		String tableName = tableNamePrefix+httpRequest.getUrlParametrs().get("table");
		
		if(httpRequest.getUrl().startsWith("/api/select")) {
			
			List<SqlExpression> whereData = DataBaseInterface.parseWhere(new JSONArray(httpRequest.getContent()));
			result = new SqlSelect(tableName, whereData);
			
		} else if(httpRequest.getUrl().startsWith("/api/insert")) {
			
			List<CellData> insertData = DataBaseInterface.parseCellDataRow(httpRequest.getContent());
			result = new SqlInsert(tableName, insertData);
			
		} else if(httpRequest.getUrl().startsWith("/api/update")) {
			
			String jsonUpdateData = httpRequest.getContent();
			JSONObject updateData = new JSONObject(jsonUpdateData);
			
			List<SqlExpression> whereData = DataBaseInterface.parseWhere(updateData.getJSONArray("where"));
			List<CellData> setData = DataBaseInterface.parseCellDataRow(updateData.getJSONArray("set"));
			
			result = new SqlUpdate(tableName, whereData, setData);
			
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
		String responseContent = "";
		if(!simpleValidation(new String[] {"api_key", "table"}, httpRequest.getUrlParametrs())) {
			sendValidationFailResponse(ctx, httpResponse);
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
		
		if(sqlRequest instanceof SqlInsert && httpRequest.getUrlParametrs().containsKey("updateIfExists") && httpRequest.getUrlParametrs().containsKey("checkField1")) {
			
			Row insertData = new Row(DataBaseInterface.parseCellDataRow(httpRequest.getContent()));
			List<SqlExpression> whereExpression = new ArrayList<>();
			int i = 1;
				
			while (httpRequest.getUrlParametrs().containsKey("checkField"+i)) {
				String fieldName = httpRequest.getUrlParametrs().get("checkField"+(i++));
				if(!insertData.containsCell(fieldName)) {
					httpResponse.setContent(simpleJsonObject("Error", "Field name mismatch"));
					sendHttpResponse(ctx, httpResponse);
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
		}
		
		httpResponse.setContent(responseContent);
		sendHttpResponse(ctx, httpResponse);
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
	
	/*private boolean validateApi(String[] names, Map<String, String> parameters, String apiKeyVarName) throws SQLException {
		if(!simpleValidation(new String[] {"player_id", "api_key"}, parameters)) {
			return false;
		}
		
		if(apiKeyVarName == null || parameters.get(apiKeyVarName) == null  || dbManager.getGameByKey(parameters.get(apiKeyVarName)) == null) {
			return false;
		}
		
		return true;
	}*/
	
	private void sendValidationFailResponse(ChannelHandlerContext ctx, HttpResponse httpResponse) {
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
		/*HttpResponse httpResponse = new HttpResponse("1.1", status, content);
		httpResponse.addHeaders(headers);*/
		ctx.writeAndFlush(Unpooled.copiedBuffer(httpResponse.toString(), CharsetUtil.UTF_8));
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}

}
