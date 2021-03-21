package com.cm.dataserver.handlers;

import com.cm.dataserver.StringDataHelper;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class RootHandler {
	protected void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
		ctx.writeAndFlush(httpResponse);
	}
	
	protected void sendValidationFailResponse(ChannelHandlerContext ctx) {
		sendHttpResponse(ctx, HttpResponseTemplates.validationFailResponse(ctx));
	}
}
