package com.cm.dataserver.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cm.dataserver.Authorization;
import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.UriAnnotation;
import com.cm.dataserver.basedbclasses.Field;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TableTemplate;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.GameTemplate;
import com.cm.dataserver.dbengineclasses.Owner;
import com.cm.dataserver.helpers.EmailSender;
import com.cm.dataserver.helpers.HttpResponseTemplates;
import com.cm.dataserver.helpers.RandomKeyGenerator;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class SystemHandler extends RootHandler {
	
	private Connection dbConnection;
	private GameTemplate math3Template;
	private EmailSender emailSender;
	
	public SystemHandler(Connection dbConnection, GameTemplate math3Template, EmailSender emailSender) {
		this.dbConnection = dbConnection;
		this.math3Template = math3Template;
		this.emailSender = emailSender;
	}
	
	@UriAnnotation(uri="/system/register_game")
	public void handleRegisterGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException {
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
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "Game alredy exists", HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		Owner owner = DataBaseMethods.getOwnerByEmail(email, dbConnection);
		
		if(owner == null) {
			int result = DataBaseMethods.regOwner(email, dbConnection);
			
			if(result <= 0) {
				sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "Error occurred during registration", HttpResponseStatus.INTERNAL_SERVER_ERROR));
				return;
			}
			
			owner = DataBaseMethods.getOwnerByEmail(email, dbConnection);
		}
		
		dbConnection.setAutoCommit(false);
		String prefix = DataBaseMethods.generateTablePrefix(gameName, apiKey);
		int added = DataBaseMethods.insertGame(gameName, gameType, owner.getId(), apiKey, apiSecret, prefix, Authorization.generateHmacHash(apiSecret, apiKey), dbConnection);
		
		if(added < 1) {
			dbConnection.rollback();
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "An error occurred while adding the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		if("Yes".equals(bodyParameters.get("match3"))) {
			DataBaseMethods.createGameTables(math3Template, prefix, dbConnection); //throw exception if not successfully
		} else {
			DataBaseMethods.createGameTable(new TableTemplate("players", new Field[] {new Field(Types.VARCHAR, "playerId").setLength(17).defNull(false), new Field(Types.VARCHAR, "facebookId")}, "playerId"), prefix, dbConnection);
		}
		
		dbConnection.commit();
		dbConnection.setAutoCommit(true);
		
		List<Game> allGames = DataBaseMethods.getAllGames(dbConnection);
		StringBuilder viewSql = new StringBuilder();
		for (int i = 0; i < allGames.size(); i++) {
			
			Game game = allGames.get(i);
			String sql = "SELECT\r\n"
					+ game.getId()+" game_id,\r\n"
					+ "rs.uri, epsilon_test.\r\n"
					+ "rs.count \r\n"
					+ "FROM \r\n"
					+ game.getPrefix()+"requests_statistic rs";
			viewSql.append(sql);
			
			if(i < allGames.size()-1) {
				viewSql.append("\r\nUNION\r\n");
			}
		}
		
		SqlMethods.createView("requests_statistic", viewSql.toString(), dbConnection);
		
		//Game game = DataBaseMethods.getGameByKey(apiKey, connection);
		if("Yes".equals(bodyParameters.get("send_mail"))) {
			emailSender.asyncSend(email, "Your game registerd", "Game name: \""+gameName+"\" ApiKey: "+apiKey+" Api secret: "+apiSecret);
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Success", "Register successed", HttpResponseStatus.OK));
		} else {
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("game_name", gameName);
			response.put("api_key", apiKey);
			response.put("api_secret", apiSecret);
			sendHttpResponse(ctx, HttpResponseTemplates.response(StringDataHelper.jsonObject(response), HttpResponseStatus.OK));
		}
	}
}
