package com.cm.dataserver.handlers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.Authorization;
import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.UriAnnotation;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.helpers.HttpResponseTemplates;
import com.cm.dataserver.template1classes.BoostsUpdate;
import com.cm.dataserver.template1classes.GameMethods;
import com.cm.dataserver.template1classes.LevelsUpdate;
import com.cm.dataserver.template1classes.LifeRequest;
import com.cm.dataserver.template1classes.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class GameHandler extends RootHandler {
	
	@UriAnnotation(uri="/game/levels")
	public void levels(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
		
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
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse("{udpated : "+lvlUpdated+", inserted: "+lvlIserted+"}", HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/boosts")
	public void boosts(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
		
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
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse("{udpated : "+bstUpdated+", inserted: "+bstIserted+"}", HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/leaderboard")
	public void leadboard(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException {
		
		String playersTableName = game.getPrefix()+"players";
		String levelsTableName = game.getPrefix()+"levels";
		
		String sql = "SELECT p.facebookId, max(l.level) max_lvl FROM %s l " + 
					 "JOIN %s p ON p.playerId = l.playerId " + 
					 "GROUP BY p.facebookId " + 
					 "ORDER BY max_lvl DESC";
		
		List<Row> rows = SqlMethods.select(String.format(sql, levelsTableName, playersTableName), dbConnection);
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(Row.rowsToJson(rows), HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/playerprogress")
	public void playerProgress(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
		JSONObject filter = new JSONObject(inputContent);
		
		JSONArray fbIds = filter.has("f_ids")?filter.getJSONArray("f_ids"):null;
		int level = filter.has("level")?filter.getInt("level"):0;
		
		if(fbIds == null || fbIds.length() <= 0 || level <= 0) {
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "Filter fail", HttpResponseStatus.BAD_REQUEST));
			return;
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
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(Row.rowsToJson(rows1), HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/maxplayerprogress")
	public void maxPlayerProgress(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
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
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(Row.rowsToJson(rows2), HttpResponseStatus.OK));
	}
	
	enum LifeRequestCreationType { NORMAL, SEND_LIFE }
	
	private void handleCreateLifeRequest(ChannelHandlerContext ctx, String inputContent, String gamePrefix, LifeRequestCreationType status, String playerId, Connection dbConnection) throws JSONException, SQLException {
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
				lifeRequestList.add(new LifeRequest(StringDataHelper.generateBigId(), lifeSender.getPlayerId(), playerId, lifeRequestStatus));
			} else if(status == LifeRequestCreationType.SEND_LIFE) {
				lifeRequestList.add(new LifeRequest(StringDataHelper.generateBigId(), playerId, lifeSender.getPlayerId(), lifeRequestStatus));
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
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Success", "Life reuqests created successfully", HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/create_life_request")
	public void createLifeRequest(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
		handleCreateLifeRequest(ctx, inputContent, game.getPrefix(), LifeRequestCreationType.NORMAL, playerId.getValue(), dbConnection);
	}
	
	@UriAnnotation(uri="/game/send_life")
	public void sendLife(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws JSONException, SQLException {
		
		//
		// This case create 'confirmed' life request, that to be able to send life directly to player, without opened request creation
		//
		handleCreateLifeRequest(ctx, inputContent, game.getPrefix(), LifeRequestCreationType.SEND_LIFE, playerId.getValue(), dbConnection);
	}
	
	private void handleLifeRequestUpdateStatus(ChannelHandlerContext ctx, String inputContent, String gamePrefix, String status, Connection dbConnection) throws SQLException, JSONException {
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
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "An error occurred while life request status upadte", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Success", "Life reuqest statuss updated successfully", HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/game/confirm_life_request")
	public void confirmLifeRequest(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		handleLifeRequestUpdateStatus(ctx, inputContent, game.getPrefix(), "confirm", dbConnection);
	}
	
	@UriAnnotation(uri="/game/deny_life_request")
	public void denyLifeRequest(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		handleLifeRequestUpdateStatus(ctx, inputContent, game.getPrefix(), "deny", dbConnection);
	}
	
	@UriAnnotation(uri="/game/accept_life")
	public void acceptLifeRequest(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		handleLifeRequestUpdateStatus(ctx, inputContent, game.getPrefix(), "deny", dbConnection);
	}
	
	@UriAnnotation(uri="/game/refuse_life")
	public void refuseLifeRequest(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		handleLifeRequestUpdateStatus(ctx, inputContent, game.getPrefix(), "deny", dbConnection);
	}
	
	@UriAnnotation(uri="/game/life_requests")
	public void lifeRewuests(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		JSONObject lifeRequests = GameMethods.getLifeRequests(game.getPrefix(), playerId.getValue(), dbConnection);
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(lifeRequests.toString(), HttpResponseStatus.OK));
	}
}
