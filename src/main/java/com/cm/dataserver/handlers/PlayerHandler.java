package com.cm.dataserver.handlers;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.UriAnnotation;
import com.cm.dataserver.dbengineclasses.DataBaseMethods;
import com.cm.dataserver.dbengineclasses.Game;
import com.cm.dataserver.template1classes.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class PlayerHandler extends RootHandler {
	
	private Connection dbConnection;
	
	public PlayerHandler(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}
	
	@UriAnnotation(uri="/player/authorization")
	public void handlePlayerRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, Game game) throws InvalidKeyException, NoSuchAlgorithmException, SQLException {
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
				sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("playerId", player.getPlayerId(), HttpResponseStatus.OK));
				return;
			}
			
			String playerId = DataBaseMethods.registrationPlayerByFacebookId(contentParameters.get("facebookId"), game.getPrefix(), dbConnection);
			if(playerId != null) {
				sendHttpResponse(ctx, HttpResponseTemplates.buildSimpleResponse("playerId", playerId, HttpResponseStatus.OK));
			} else {
				sendValidationFailResponse(ctx);
			}
		}
	}
}
