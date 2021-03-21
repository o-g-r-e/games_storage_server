package com.cm.dataserver.template1classes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.StringDataHelper;
import com.cm.dataserver.basedbclasses.CellData;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TypedValueArray;
import com.cm.dataserver.basedbclasses.queryclasses.SimpleSqlExpression;

public class GameMethods {
	/*public static List<PlayerMessage> getPlayerMessages(String gamePrefix, String playerId, Connection connection) throws SQLException {
		List<PlayerMessage> result = new ArrayList<>();
		
		List<Row> rows = SqlMethods.select(  "SELECT player_messages.id, player_messages.type, facebookId AS sender_facebook_id, player_messages.message_content FROM players\r\n"
										   + "    JOIN ( SELECT id, type, sender_id, message_content FROM messages WHERE recipient_id=? ) player_messages\r\n"
										   + "ON playerId = player_messages.sender_id", new QueryTypedValue(playerId), connection);
		
		for (Row row : rows) {
			result.add(new PlayerMessage(row.getString("id"), 
										 row.getString("sender_facebook_id"), 
										 row.getString("type"), 
										 row.getString("message_content")));
		}
		return result;
	}*/
	
	public static int insertMessage(String gamePrefix, String type, String sederId, String recipientId, String messageContent, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(type, sederId, recipientId, messageContent);
		return SqlMethods.insert("INSERT INTO "+gamePrefix+"messages (type, sender_id, recipient_id, message_content) VALUES (?,?,?,?)", typedValueArray.getQueryValues(), connection);
	}
	
	public static List<Row> getPlayerMessages(String gamePrefix, String playerId, Connection connection) throws SQLException {
		return SqlMethods.select(  "SELECT player_messages.id, player_messages.type, facebookId AS sender_facebook_id, player_messages.message_content FROM "+gamePrefix+"players\r\n"
										   + "    JOIN ( SELECT id, type, sender_id, message_content FROM "+gamePrefix+"messages WHERE recipient_id=? ) player_messages\r\n"
										   + "ON playerId = player_messages.sender_id", new QueryTypedValue(playerId), connection);
	}
	
	public static int deleteMessage(String gamePrefix, String playerId, String messageId, Connection connection) throws SQLException {
		return SqlMethods.delete("DELETE FROM "+gamePrefix+"messages WHERE id=? AND recipient_id=?", new TypedValueArray(messageId, playerId).getQueryValues(), connection);
	}
	
	public static int createLifeRequest(String gamePrefix, String requestId, String lifeSender, String lifeReceiver, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(requestId, lifeSender, lifeReceiver);
		return SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES (?,?,?,'open')", typedValueArray.getQueryValues(), connection);
	}
	
	public static int createLifeRequests(String gamePrefix, List<LifeRequest> lifeRequestList, Connection connection) {
		
		if(gamePrefix == null || lifeRequestList == null || connection == null || lifeRequestList.size() <= 0) {
			return 0;
		}
		
		/*TypedValueArray typedValueArray = new TypedValueArray();
		for (LifeRequest lifeRequest : lifeRequestList) {
			typedValueArray.addValues(lifeRequest.getId(), lifeRequest.getLifeSenderId(), lifeRequest.getLifeReceiverId(), lifeRequest.getStatus());
		}
		String sqlValuesPart = repeat("(?,?,?,?)", ",", lifeRequestList.size());
		return SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES "+sqlValuesPart, typedValueArray.getQueryValues(), connection);*/
		
		//
		// Separate SQL INSERT requests to avoid dublicate key exception, which appear try create repeated request to same player:
		//
		
		int inserted = 0;
		for (LifeRequest lifeRequest : lifeRequestList) {
			try {
				inserted += SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES (?,?,?,?)", new TypedValueArray(lifeRequest.getId(), lifeRequest.getLifeSenderId(), lifeRequest.getLifeReceiverId(), lifeRequest.getStatus()).getQueryValues(), connection);
			} catch (SQLException e) {
				//e.printStackTrace();
			}
		}
		return inserted;
	}
	
	//public static int createConfirmedLifeRequest(String gamePrefix, String requestId, String lifeSender, String lifeReceiver, Connection connection) throws SQLException {
		
		//
		// This method create 'confirmed' life request, that to be able to send life directly to player, without opened request creation
		//
		
		//TypedValueArray typedValueArray = new TypedValueArray(requestId, lifeSender, lifeReceiver);
		//return SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES (?,?,?,'confirmed')", typedValueArray.getQueryValues(), connection);
	//}
	
	public static int confirmLifeRequest(String gamePrefix, String requestId, Connection connection) throws SQLException {
		return SqlMethods.update("UPDATE "+gamePrefix+"life_requests SET status='confirmed' WHERE id=?", new TypedValueArray(requestId).getQueryValues(), connection);
	}
	
	public static int confirmLifeRequests(String gamePrefix, List<String> lifeRequestIdList, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray();
		for (String lifeRequestId : lifeRequestIdList) {
			typedValueArray.addValue(lifeRequestId);
		}
		String updateWherePart = StringDataHelper.repeat("id=?", " OR ", lifeRequestIdList.size());
		return SqlMethods.update("UPDATE "+gamePrefix+"life_requests SET status='confirmed' WHERE "+updateWherePart, typedValueArray.getQueryValues(), connection);
	}
	
	public static int denyLifeRequest(String gamePrefix, String requestId, Connection connection) throws SQLException {
		return SqlMethods.delete("DELETE FROM "+gamePrefix+"life_requests WHERE id=?", new TypedValueArray(requestId).getQueryValues(), connection);
	}
	
	public static int denyLifeRequests(String gamePrefix, List<String> lifeRequestIdList, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray();
		for (String lifeRequestId : lifeRequestIdList) {
			typedValueArray.addValue(lifeRequestId);
		}
		String updateWherePart = StringDataHelper.repeat("id=?", " OR ", lifeRequestIdList.size());
		return SqlMethods.delete("DELETE FROM "+gamePrefix+"life_requests WHERE "+updateWherePart, typedValueArray.getQueryValues(), connection);
	}
	
	public static JSONObject getLifeRequests(String gamePrefix, String playerId, Connection connection) throws SQLException, JSONException {
		StringBuilder resultJSON = new StringBuilder("{\"requests_for_you_lives\":[");
		
		List<Row> openedRequestsForPlayer =  SqlMethods.select("SELECT requests.id AS request_id, facebookId AS player_facebook_id  FROM "+gamePrefix+"players\r\n"
									+ "JOIN (SELECT * FROM "+gamePrefix+"life_requests WHERE life_sender=? AND status='open') requests\r\n"
									+ "ON playerId = requests.life_receiver", new QueryTypedValue(playerId), connection);
		
		List<Row> confirmedPlayerRequests =  SqlMethods.select("SELECT requests.id AS request_id, facebookId AS player_facebook_id  FROM "+gamePrefix+"players\r\n"
				+ "JOIN (SELECT * FROM "+gamePrefix+"life_requests WHERE life_receiver=? AND status='confirmed') requests\r\n"
				+ "ON playerId = requests.life_sender", new QueryTypedValue(playerId), connection);
		
		for (int i = 0; i < openedRequestsForPlayer.size(); i++) {
			Row r = openedRequestsForPlayer.get(i);
			resultJSON.append("{\"request_id\":\"").append(r.get("request_id")).append("\",\"player_facebook_id\":\"").append(r.get("player_facebook_id")).append("\"}");
			if(i<openedRequestsForPlayer.size()-1) resultJSON.append(",");
		}
		
		resultJSON.append("],\"your_approved_lives\":[");
		
		for (int i = 0; i < confirmedPlayerRequests.size(); i++) {
			Row r = confirmedPlayerRequests.get(i);
			resultJSON.append("{\"request_id\":\"").append(r.get("request_id")).append("\",\"from_facebook_id\":\"").append(r.get("player_facebook_id")).append("\"}");
			if(i<confirmedPlayerRequests.size()-1) resultJSON.append(",");
		}
		
		resultJSON.append("]}");
		
		return new JSONObject(resultJSON.toString());
	}

}