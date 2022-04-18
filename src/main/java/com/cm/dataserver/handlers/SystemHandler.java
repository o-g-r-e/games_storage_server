package com.cm.dataserver.handlers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.cm.dataserver.helpers.Settings;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class SystemHandler extends RootHandler {
	
	private Connection dbConnection;
	private EmailSender emailSender;
	private static final GameTemplate MATCH_3_TEMPLATE = GameTemplate.match3Template();
	private static final GameTemplate CASUAL_TEMPLATE = GameTemplate.gameWithEventsSystemTemplate();
	private boolean allowTestInvoice;
	
	public SystemHandler(Connection dbConnection, EmailSender emailSender, boolean allowTestInvoice) {
		this.dbConnection = dbConnection;
		this.emailSender = emailSender;
		this.allowTestInvoice = allowTestInvoice;
	}
	
	private boolean checkInvoice(String invoice) throws MalformedURLException, IOException, JSONException {
		
		if(allowTestInvoice && "IN121212121210".equals(invoice)) return true;

		String serivceUrl = "https://api.assetstore.unity3d.com";
		String key = Settings.invoiceKey;
		String requestPath = "/publisher/v1/invoice/verify.json";
		String requestParameters = "key=%s&invoice=%s";
		
		HttpURLConnection conn = (HttpURLConnection) new URL(serivceUrl + requestPath + "?" + String.format(requestParameters, key, invoice)).openConnection();
		conn.connect();
		String response = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining("\n"));
		JSONObject json = new JSONObject(response);
		
		JSONArray invoicesArray = json.getJSONArray("invoices");
		
		if(invoicesArray.length() <= 0) {
			return false;
		}
		
		String responseDownloaded = invoicesArray.getJSONObject(0).getString("downloaded");
		String responseRefunded = invoicesArray.getJSONObject(0).getString("refunded");
		
		return "Yes".equals(responseDownloaded) && "No".equals(responseRefunded);
	}
	
	@UriAnnotation(uri="/system/games")
	public void games(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException, JSONException {
		Map<String, String> bodyParameters = StringDataHelper.parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!StringDataHelper.simpleValidation(new String[] {"email"}, bodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		String email = bodyParameters.get("email");
		
		List<Game> ownerGames = DataBaseMethods.getOwnerGames(dbConnection, new String[] {"id", "name"},  email);
		
		StringBuilder jsonGames = new StringBuilder("[");
		
		for (int i = 0; i < ownerGames.size(); i++) {
			Game g = ownerGames.get(i);
			jsonGames.append(String.format("{\"%s\":%d,\"%s\":\"%s\"}", "id", g.getId(), "name", g.getName()));
			if(i<ownerGames.size()-1) jsonGames.append(",");
		}
		
		sendHttpResponse(ctx, HttpResponseTemplates.response(jsonGames.toString(), HttpResponseStatus.OK));
	}

	private boolean isTrueValue(String val) {
		if(val != null && "true".equals(val)) {
			return true;
		}

		return false;
	}
	
	@UriAnnotation(uri="/system/register_game")
	public void handleRegisterGame(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, FileNotFoundException, ClassNotFoundException, IOException, JSONException {
		Map<String, String> bodyParameters = StringDataHelper.parseParameters(httpRequest.content().toString(CharsetUtil.UTF_8));
		
		if(!StringDataHelper.simpleValidation(new String[] {"game_name", "email", "send_mail", "invoice"}, bodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}

		boolean withEvents = isTrueValue(bodyParameters.get("with_events"));
		String gameName = bodyParameters.get("game_name");
		String email = bodyParameters.get("email");
		String invoice = bodyParameters.get("invoice");
		
		if(!StringDataHelper.validateGameCreationParameters(gameName, email, invoice)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		if(!checkInvoice(invoice)) {
			sendBadInvoiceResponse(ctx);
			return;
		}
		
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

		int added = DataBaseMethods.insertGame(gameName, withEvents?"match3_WithEvents":"match3", owner.getId(), apiKey, apiSecret, prefix, Authorization.generateHmacHash(apiSecret, apiKey), dbConnection);
		
		if(added < 1) {
			dbConnection.rollback();
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "An error occurred while adding the game", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		if(withEvents) {
			DataBaseMethods.createGameTables(CASUAL_TEMPLATE, prefix, dbConnection);
		} else {
			DataBaseMethods.createGameTables(MATCH_3_TEMPLATE, prefix, dbConnection); //throw exception if not successfully
		}
		
		dbConnection.commit();
		dbConnection.setAutoCommit(true);
		
		List<Game> allGames = DataBaseMethods.getAllGames(dbConnection);
		StringBuilder viewSql = new StringBuilder();
		for (int i = 0; i < allGames.size(); i++) {
			
			Game game = allGames.get(i);
			String sql = "SELECT\r\n"
					+ game.getId()+" game_id,\r\n"
					+ "rs.uri,\r\n"
					+ "rs.count \r\n"
					+ "FROM \r\n"
					+ game.getPrefix()+"requests_statistic rs";
			viewSql.append(sql);
			
			if(i < allGames.size()-1) {
				viewSql.append("\r\nUNION\r\n");
			}
		}
		
		//SqlMethods.createView("requests_statistic", viewSql.toString(), dbConnection);
		
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
