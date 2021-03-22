package com.cm.dataserver.handlers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.UriAnnotation;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.dbengineclasses.PlayerId;
import com.cm.dataserver.helpers.HttpResponseTemplates;
import com.cm.dataserver.template1classes.GameMethods;
import com.cm.dataserver.template1classes.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MessagesHandler extends RootHandler {
	
	@UriAnnotation(uri="/message/send")
	public void sendMessage(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException {
		Map<String, String> bodyParameters = StringDataHelper.parseParameters(inputContent);
		
		if(!StringDataHelper.simpleValidation(new String[] {"type", "recipient_facebook_id", "message"}, bodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		Player messageRecipient = DataBaseMethods.getPlayerByFacebookId(bodyParameters.get("recipient_facebook_id"), game.getPrefix(), dbConnection);
		
		int added = GameMethods.insertMessage(game.getPrefix(), bodyParameters.get("type"), playerId.getValue(), messageRecipient.getPlayerId(), bodyParameters.get("message"), dbConnection);
		
		if(added < 1) {
			dbConnection.rollback();
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "An error occurred while adding message", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Success", "Message sent successfully", HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/message/fetch_my_messages")
	public void geMessages(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException {
		List<Row> playerMessages = GameMethods.getPlayerMessages(game.getPrefix(), playerId.getValue(), dbConnection);
		sendHttpResponse(ctx, HttpResponseTemplates.buildResponse(Row.rowsToJson(playerMessages), HttpResponseStatus.OK));
	}
	
	@UriAnnotation(uri="/message/delete")
	public void delete(ChannelHandlerContext ctx, String inputContent, Game game, PlayerId playerId, Connection dbConnection) throws SQLException {
		Map<String, String> mDeletionBodyParameters = StringDataHelper.parseParameters(inputContent);
		
		if(!StringDataHelper.simpleValidation(new String[] {"id"}, mDeletionBodyParameters)) {
			sendValidationFailResponse(ctx);
			return;
		}
		
		int deleted = GameMethods.deleteMessage(game.getPrefix(), playerId.getValue(), mDeletionBodyParameters.get("id"), dbConnection);
		
		if(deleted < 1) {
			sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Error", "An error occurred while deletion message", HttpResponseStatus.INTERNAL_SERVER_ERROR));
			return;
		}
		sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("Success", "Message deleted successfully", HttpResponseStatus.OK));
	}
}
