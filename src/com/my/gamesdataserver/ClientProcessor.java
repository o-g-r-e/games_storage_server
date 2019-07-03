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

public class ClientProcessor extends Thread {
	
	private BufferedReader in;
	private PrintWriter out;
	
	private Socket socket;
	private DataBaseManager dbm = null;
	private String ipAddress;
	
	private HTTPRequestFilter httpRequestFilter;
	private Access access;
	
	public ClientProcessor(Socket socket, DataBaseManager dbm) throws IOException {
		this.socket = socket;
		this.dbm = dbm;
		this.ipAddress = socket.getRemoteSocketAddress().toString();
		this.httpRequestFilter = new HTTPRequestFilter();
		this.access = new Access();
		System.out.println("\nNew connection from "+this.ipAddress);
	    
	}

	public void run() {
		//System.out.println("Client socket started.");
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String httpRequest = readRequest(in);
			String url = parseUrl(httpRequest);
			String urlPath = parseUrlPath(url);
			
			if(httpRequestFilter.filterForbiddens(urlPath))  {
				System.out.println("Request parsing failed or filtered.");
				return;
			}
			
			System.out.println("\nRequest from "+ipAddress);
			System.out.println(httpRequest.toString());
			
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			
			Request request = new Request(url);
			
			if(request.commandValidate()) {
				commandRequestProcessor(request, out);
			} else {
				contentRequestProcessor(urlPath, out);
			}
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
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
	}
	
	private String readRequest(Reader reader) throws IOException {
		StringBuilder result = new StringBuilder();
		
		String line;
		while(!(line = in.readLine()).equals("")) {
			result.append(line).append("\n");
		}
		
		return result.toString();
	}
	
	private String parseUrl(String httpRequest) {
		String result = null;
		Pattern urlPattern = Pattern.compile("GET ([\\w\\?=&/%{}\\.-]+) HTTP");
		Matcher matcher = urlPattern.matcher(httpRequest);
		
		if(matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	private String parseUrlPath(String url) {
		Pattern p = Pattern.compile("\\/([\\w\\.\\/]+)");
		Matcher m = p.matcher(url);
		
		if(m.find()) {
			return m.group(1);
		}
		return null;
	}
	
	private void contentRequestProcessor(String urlPath, PrintWriter out) throws IOException {
		if(access.isAllowedPath(urlPath)) {
			String workPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getCanonicalPath();
			String content = readFile(workPath+File.separatorChar+"epsilon_monitor"+urlPath.replaceAll("/", String.valueOf(File.separatorChar)), StandardCharsets.UTF_8);
			sendHttpResponse(out,content);
		}
	}
	
	private void commandRequestProcessor(Request request, PrintWriter out) throws IOException, JSONException {
		
		Map<String, String> getParameters = request.getParameters();
		Commands command = request.getCommand();
		
		switch (command) {
		case READ_SAVE:
			try {
				GameSaveData saveData = dbm.selectSaves(getParameters.get("key"), getParameters.get("player_id"));
				if(saveData != null) {
					String savesJson = saveData.toJsonString();
					sendHttpResponse(out, savesJson);
				} else {
					sendHttpResponse(out, simpleJsonObject("sqlMessage", "0 row(s) returned"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
			}
			
			break;
		case REGISTER_PLAYER:
			try {
				int rowsAdded = dbm.insertPlayer(getParameters.get("name"), getParameters.get("player_id"));
				sendHttpResponse(out, simpleJsonObject("addedRows", String.valueOf(rowsAdded)));
			} catch (SQLException e1) {
				e1.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e1.getMessage()));
			}
			break;
		case ADD_GAME:
			try {
				RandomKeyGenerator rkg = new RandomKeyGenerator(45);
				String key = rkg.nextString();
				int rowsAdded = dbm.insertGame(getParameters.get("owner_name"), getParameters.get("name"), key);
				if(rowsAdded > 0) {
					sendHttpResponse(out, simpleJsonObject("security_key", key));
				} else {
					sendHttpResponse(out, simpleJsonObject("error", "Game not added."));
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e1.getMessage()));
			}
			break;
		case UPDATE_LEVEL:
			try {
				int rowsChanged = dbm.updateLevel(getParameters.get("key"), getParameters.get("player_id"), Integer.parseInt(getParameters.get("level")), Integer.parseInt(getParameters.get("stars")));
				if(rowsChanged > 0) {
					sendHttpResponse(out, simpleJsonObject("updatedRows", String.valueOf(rowsChanged)));
				} else {
					sendHttpResponse(out, simpleJsonObject("error", "Level data not updated."));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
			}
			break;
		case INSERT_LEVEL:
			try {
				int rowsAdded = dbm.insertLevel(getParameters.get("key"), getParameters.get("player_id"), Integer.parseInt(getParameters.get("level")), Integer.parseInt(getParameters.get("stars")));
				if(rowsAdded > 0) {
					sendHttpResponse(out, simpleJsonObject("addedRows", String.valueOf(rowsAdded)));
				} else {
					sendHttpResponse(out, simpleJsonObject("error", "Level data not added."));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
			}
			break;
		case REGISTER_OWNER:
			try {
				int rowsChanged = dbm.insertOwner(getParameters.get("name"));
				sendHttpResponse(out, simpleJsonObject("addedRows", String.valueOf(rowsChanged)));
			} catch (SQLException e) {
				e.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
			}
			break;
		case UPDATE_BOOST:
			try {
				int rowsChanged = dbm.updateBoostData(getParameters.get("key"), getParameters.get("player_id"), URLDecoder.decode(getParameters.get("boost_data"), "UTF-8"));
				if(rowsChanged > 0) {
					sendHttpResponse(out, simpleJsonObject("updatedRows", String.valueOf(rowsChanged)));
				} else {
					sendHttpResponse(out, simpleJsonObject("error", "Boost data not updated."));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				sendHttpResponse(out, simpleJsonObject("sqlError", e.getMessage()));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			break;
		/*case "eps_mon":
			try {
				List<BoostEntity> boosts = dbm.selectFromBoosts();
				List<GameOwnerEntity> gameOwners = dbm.selectFromGameOwners();
				List<GameEntity> games = dbm.selectFromGames();
				List<PlayerEntity> players = dbm.selectFromPlayers();
				List<SaveEntity> saves = dbm.selectFromSaves();
				
				String response = prepareJsonForMonitor(boosts, gameOwners, games, players, saves);
				
				//sendHttp(out, response.append("{").append(buildJson(boosts, "boosts")).append(",").append(buildJson(gameOwners, "game_owners")).append(",").append(buildJson(games, "games")).append(",").append(buildJson(players, "players")).append(",").append(buildJson(saves, "saves")).append("}").toString());
				//sendHttp(out,"{\"boosts\":[[1,1,1,1]],\"game_owners\":[[1,Yuriy]],\"games\":[[1,match3,1,abc]],\"players\":[[1,Yuriy,123]],\"saves\":[[1,1,1,1,3]]}");
				sendHttp(out, response.toString());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			break;*/
		/*case SHOW_MON:
			String workPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getCanonicalPath();
			String content = readFile(workPath+File.separatorChar+"epsilon_monitor"+File.separatorChar+"monitor.html", StandardCharsets.UTF_8);
			sendHttpResponse(out,content);
			break;*/
		/*default:
			if(httpRequestFilter.filterAllowed(request.getPath())) {
				String workPath2 = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getCanonicalPath();
				String content2 = readFile(workPath2+File.separatorChar+"epsilon_monitor"+File.separatorChar+request.getPath(), StandardCharsets.UTF_8);
				sendHttpResponse(out,content2);
			}
			break;*/
		}
    }
	
	/*private String prepareJsonForMonitor(List<BoostEntity> boosts, List<GameOwnerEntity> gameOwners, List<GameEntity> games, List<PlayerEntity> players, List<SaveEntity> saves) {
		
		StringBuilder result = new StringBuilder();
		result.append("{\"boosts\":[");
		
		for(int i=0;i<boosts.size();i++) {
			result.append(boosts.get(i).toJson());
			if(i<boosts.size()-1) {
				result.append(",");
			}
		}
		
		result.append("],\"game_owners\":[");
		
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
	}*/
	
	String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
    
	void sendHttpResponse(PrintWriter writer, String httpContent) {
		System.out.println("\nHTTP response to: "+ipAddress);
		if(httpContent.length() > 30) {
			System.out.println(httpContent.substring(0, 30));
		} else {
			System.out.println(httpContent);
		}
		writer.println("HTTP/1.1 200 OK\r\n" +
           		"Content-Length: "+httpContent.length()+"\r\n"
           		+ "\r\n"
           		+ httpContent);
	}
	
	private String simpleJsonObject(String name, String value) {
		return "{ \""+name+"\" : \""+value+"\" }";
	}
}
