package com.my.gamesdataserver;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.dbmodels.GameEntity;
import com.my.gamesdataserver.dbmodels.GameOwnerEntity;
import com.my.gamesdataserver.dbmodels.SaveEntity;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import com.my.gamesdataserver.dbmodels.PlayerEntity;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	
	private DataBaseManager dbm = null;
	
	private HTTPRequestFilter httpRequestFilter;
	private Access access;
	
	public ClientHandler(DataBaseManager dbm) throws IOException {
		this.dbm = dbm;
		this.httpRequestFilter = new HTTPRequestFilter();
		this.access = new Access();
		//System.out.println("\nNew connection from "+this.ipAddress);
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
		System.out.println("channelRead");
		String httpRequest;
	    try {
	    	httpRequest = readRequest((ByteBuf)msg);
	    } finally {
	        ReferenceCountUtil.release(msg);
	    }
	    
	    String url = parseUrl(httpRequest);
		String urlPath = parseUrlPath(url);
		
		if(httpRequestFilter.filterForbiddens(urlPath))  {
			System.out.println("Request parsing failed or filtered.");
			return;
		}
		
		printHttpRequest(httpRequest, "nnn.nnn.nnn.nnn", false);
		
		Request request = new Request(url);
		
		try {
			if(request.validateParametersWithSchema()) {
				commandRequestProcessor(request, ctx);
			} else if(access.isAllowedPath(urlPath) && url.contains("key="+Access.contentAccessKey)) {
				contentRequestProcessor(urlPath, ctx);
			} else {
				sendHttpResponse(ctx, simpleJsonObject("Request error", "Bad request"));
				return;
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
			sendHttpResponse(ctx, simpleJsonObject("sqlError", e.getMessage()));
		}
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
    
	private String readRequest(ByteBuf in) {
		StringBuilder result = new StringBuilder();
		
		while (in.isReadable()) {
			result.append(in.readChar());
	    }
		
		return result.toString();
	}
	
	private String parseUrl(String httpRequest) {
		String result = null;
		Pattern urlPattern = Pattern.compile("GET (.+) HTTP");
		Matcher matcher = urlPattern.matcher(httpRequest);
		
		if(matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	private String parseUrlPath(String url) {
		Pattern p = Pattern.compile("([\\w\\.\\/-]+)");
		Matcher m = p.matcher(url);
		
		if(m.find()) {
			return m.group(1);
		}
		return null;
	}
	
	private void contentRequestProcessor(String urlPath, ChannelHandlerContext ctx) throws IOException {
		String workPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getCanonicalPath().replaceAll("%20", " ");
		String content = readFile(workPath+urlPath.replace('/', File.separatorChar), StandardCharsets.UTF_8);
		sendHttpResponse(ctx, content);
	}
	
	private void commandRequestProcessor(Request request, ChannelHandlerContext ctx) throws IOException, JSONException, SQLException {
		
		Map<String, String> getParameters = request.getParameters();
		Request.Type command = request.getCommand();
		
		switch (command) {
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
		}
    }
	
	private void readSave(Map<String, String> getParameters, ChannelHandlerContext ctx) throws SQLException, JSONException {
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
	}
	
	private String prepareJsonForMonitor(List<GameOwnerEntity> gameOwners, List<GameEntity> games, List<PlayerEntity> players, List<SaveEntity> saves) {
		
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
	}
	
	String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
    
	void sendHttpResponse(ChannelHandlerContext ctx, String httpContent) {
		HttpResponse httpResponse = new HttpResponse("1.1", 200, httpContent);
		printHttpResponse(httpResponse, "nnn.nnn.nnn.nnn", true);
		ctx.writeAndFlush(httpResponse.toString());
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
	
	private void printHttpRequest(String httpRequest, String ipAddress, boolean isPrintHeader) {
		System.out.println("\nRequest from "+ipAddress);
		System.out.println(isPrintHeader ? httpRequest : httpRequest.substring(0, httpRequest.indexOf("\n")));
	}
	
	private void printHttpResponse(HttpResponse httpResponse, String ipAddress, boolean cutContent) {
		System.out.println("\nHTTP response to: "+ipAddress);
		System.out.println(httpResponse.getHeader());
		if(httpResponse.getContent() != null && httpResponse.getContent().length() > 0) {
			if(cutContent) {
				System.out.println(httpResponse.getContentCharacters(255));
			} else {
				System.out.println(httpResponse.getContent());
			}
		}
	}
}
