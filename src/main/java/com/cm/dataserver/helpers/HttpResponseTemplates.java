package com.cm.dataserver.helpers;

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

public class HttpResponseTemplates {
	
	public static FullHttpResponse buildSimpleResponse(String status, String message, HttpResponseStatus httpStatus) {
		return buildResponse(StringDataHelper.jsonObject(status, message), httpStatus);
	}
	
	public static FullHttpResponse response(String message, HttpResponseStatus httpStatus) {
		return buildResponse(message, httpStatus);
	}
	
	public static FullHttpResponse buildResponse(String content, HttpResponseStatus httpStatus) {
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpStatus, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
		response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		response.headers().set("Content-Type", "application/json");
		return response;
	}
	
	public static FullHttpResponse validationFailResponse(ChannelHandlerContext ctx) {
		return buildSimpleResponse("Error", "Parameters validation failed", HttpResponseStatus.BAD_REQUEST);
	}
}
