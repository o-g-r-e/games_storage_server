package com.cm.dataserver.handlers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.UriAnnotation;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.queryclasses.Select;
import com.cm.dataserver.dbengineclasses.ApiMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.helpers.HttpResponseTemplates;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ApiHandler extends RootHandler {
	
	@UriAnnotation(uri="/api/select")
	public void select(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		List<Row> rows = new ArrayList<>();
		//Select select = new Select(new JSONObject(inputContent));
		
		//if(objectQuery) {
			//rows = ApiMethods.select(select, playerId, game.getPrefix(), dbConnection);
		//} else {
			rows = ApiMethods.select(new JSONObject(inputContent), playerId, game.getPrefix(), dbConnection);
		//}
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(Row.rowsToJson(rows), HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/api/insert")
	public void insert(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		String responseContent = "";
		if(ApiMethods.insert(new JSONObject(inputContent), playerId, game.getPrefix(), dbConnection) > 0) {
			responseContent = StringDataHelper.jsonObject("Success", "Insert completed successfully");
		} else {
			responseContent = StringDataHelper.jsonObject("Error", "An error occurred while inserting");
		}
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(responseContent, HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/api/update")
	public void update(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException, JSONException {
		String responseContent = "";
		if(ApiMethods.update(new JSONObject(inputContent), playerId, game.getPrefix(), dbConnection) > 0) {
			responseContent = StringDataHelper.jsonObject("Success", "Update completed successfully");
		} else {
			responseContent = StringDataHelper.jsonObject("Error", "An error occurred while updating");
		}
		
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(responseContent, HttpResponseStatus.OK));
	}
}
