package com.cm.dataserver.dbengineclasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.basedbclasses.CellData;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TypedValueArray;
import com.cm.dataserver.basedbclasses.queryclasses.Select;
import com.cm.dataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.cm.dataserver.basedbclasses.queryclasses.SqlExpression;
import com.cm.dataserver.basedbclasses.queryclasses.SqlLogicExpression;

public class ApiMethods {
	
	private static Pattern helpPattern = Pattern.compile("\"([\\w\"]+)\"");
	
	private static String fieldsWithoutQuotes(JSONArray jsonArray) throws JSONException {
		Matcher m = helpPattern.matcher(jsonArray.join(","));
		String[] resultValues = new String[jsonArray.length()];
		
		int i=0;
		while (m.find()) 
			resultValues[i++] = m.group(1);
		
		return String.join(",", resultValues);
	}
	
	/*private static String questionMarks(int count) {
		if(count <= 0) return "";
		return new String(new char[count]).replace("\0", "?").replaceAll("\\?(?=\\?)", "?,");
	}*/
	
	private static String repeat(String string, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++) {
			result.append(string);
		}
		return result.toString();
	}
	
	/*private static String valuesArea(int valuesCount, int insertRowsCount) {
		String firstRowResult = questionMarks(valuesCount);
		
		if("".equals(firstRowResult)) {
			return "";
		}
		
		String values = "("+questionMarks(valuesCount)+")";*/
		/*values += new String(new char[jsonValues.length()-1]).replace("\0", values);
		values = values.replaceAll("\\)\\(", "),(");*/
		/*StringBuilder result = new StringBuilder((values.length()*insertRowsCount)+insertRowsCount-1);
		result.append(values);
		for (int i = 1; i < insertRowsCount; i++) {
			result.append(",").append(values);
		}
		return result.toString();
	}*/
	
	public static int insert(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = jsonQuery.getString("table");
		
		JSONArray jsonValues = jsonQuery.getJSONArray("values");
		
		StringBuilder sqlInsert = new StringBuilder("INSERT INTO ").append(tableNamePrefix+tableName);
		
		if(jsonQuery.has("fields") && jsonQuery.getJSONArray("fields").length() > 0) {
			sqlInsert.append(" (id,")
			.append(playerId.getFieldName())
			.append(",")
			.append(fieldsWithoutQuotes(jsonQuery.getJSONArray("fields")))
			.append(")");
		}
		
		sqlInsert.append(" VALUES ");
		String values = "(DEFAULT,?"+repeat(",?",jsonValues.getJSONArray(0).length())+")";
		values += repeat(","+values, jsonValues.length()-1);
		sqlInsert.append(values);
		/*String values = "("+questionMarks(jsonValues.getJSONArray(0).length())+")";
		values += new String(new char[jsonValues.length()-1]).replace("\0", values);
		values = values.replaceAll("\\)\\(", "),(");*/
		//String valuesQueryArea = valuesArea(jsonValues.getJSONArray(0).length(), jsonValues.length());
		
		
		//valuesQueryArea = valuesQueryArea.replaceAll("\\(", "(DEFAULT,?,"); //DEFAULT - auto increment id, ? - player_id
		for (int i = 0; i < jsonValues.length(); i++) {
			JSONArray jsonRow = jsonValues.getJSONArray(i);
			jsonValues.put(i, new JSONArray("['"+playerId.getValue()+"',"+jsonRow.join(",")+"]"));
		}
		
		//sqlInsert.append(valuesQueryArea);
		return SqlMethods.insert(sqlInsert.toString(), getValuesFromInsert(jsonValues), connection);
	}
	
	public static String jsonWhereToSql(JSONArray jsonWhere) throws JSONException {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < jsonWhere.length(); i++) {
			JSONArray andJsonExpression = jsonWhere.getJSONArray(i);
			
			if(i > 0) {
				result.append(" OR ");
			}
			
			for (int j = 0; j < andJsonExpression.length(); j++) {
				JSONObject jsonWhereExpression = andJsonExpression.getJSONObject(j);

				if(j > 0) {
					result.append(" AND ");
				}
				
				if(jsonWhereExpression.has("is_not")) {
					result.append(" NOT ");
				}
				
				result.append(jsonWhereExpression.getString("name")).append("=?");
			}
		}
		return result.toString();
	}
	
	private static String jsonUpdateSetToSql(JSONArray jsonSet) throws JSONException {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < jsonSet.length(); i++) {
			JSONObject jsonSetExpression = jsonSet.getJSONObject(i);
			
			if(i > 0) {
				result.append(", ");
			}
			
			result.append(jsonSetExpression.getString("name")).append("=?");
		}
		return result.toString();
	}
	
	private static List<QueryTypedValue> getValuesFromInsert(JSONArray jsonInsertValues) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonInsertValues.length(); i++) {
			JSONArray values = jsonInsertValues.getJSONArray(i);
			for (int j = 0; j < values.length(); j++) {
				result.add(new QueryTypedValue(values.get(j)));
			}
		}
		return result;
	}
	
	private static List<QueryTypedValue> getValuesFromUpdateSet(JSONArray jsonSet) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonSet.length(); i++) {
			result.add(new QueryTypedValue(jsonSet.getJSONObject(i).get("value")));
		}
		return result;
	}
	
	private static List<QueryTypedValue> getValuesFromWhere(JSONArray jsonCondition) throws JSONException {
		List<QueryTypedValue> result = new ArrayList<>();
		for (int i = 0; i < jsonCondition.length(); i++) {
			JSONArray andExpressions = jsonCondition.getJSONArray(i);
			for (int j = 0; j < andExpressions.length(); j++) {
				JSONObject jsonExpression = andExpressions.getJSONObject(j);
				result.add(new QueryTypedValue(jsonExpression.get("value")));
			}
		}
		return result;
	}
	
	public static int update(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = tableNamePrefix+jsonQuery.getString("table");
		
		StringBuilder sqlUpdate = new StringBuilder("UPDATE %s SET %s WHERE ");
		
		if(playerId == null) {
			return 0;
		}
		
		List<QueryTypedValue> whereValues = new ArrayList<>();
		
		if(jsonQuery.has("condition") && jsonQuery.getJSONArray("condition").length() > 0) {
			sqlUpdate.append(jsonWhereToSql(jsonQuery.getJSONArray("condition")));
			whereValues = getValuesFromWhere(jsonQuery.getJSONArray("condition"));
			sqlUpdate.append(" AND ");
		}
		
		if(playerId != null) {
			sqlUpdate.append(playerId.getFieldName()).append("=?");
			whereValues.add(new QueryTypedValue(playerId.getValue()));
		}
		
		List<QueryTypedValue> resultValues = getValuesFromUpdateSet(jsonQuery.getJSONArray("set"));
		resultValues.addAll(whereValues);
		
		String sql = String.format(sqlUpdate.toString(), tableName, jsonUpdateSetToSql(jsonQuery.getJSONArray("set")));
		
		return SqlMethods.update(sql, resultValues, connection);
	}
	
	public static List<Row> select(JSONObject jsonQuery, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		String tableName = tableNamePrefix+jsonQuery.getString("table");
		
		StringBuilder sql = new StringBuilder("SELECT %s FROM %s");
		
		sql.append(" WHERE ").append(playerId.getFieldName()).append("=?");
		
		if(jsonQuery.has("condition")) {
			sql.append(" AND ").append(jsonWhereToSql(jsonQuery.getJSONArray("condition")));
		}
		
		List<QueryTypedValue> resultValues = new ArrayList<>();
		resultValues.add(new QueryTypedValue(playerId.getValue()));
		
		if(jsonQuery.has("condition")) {
			resultValues.addAll(getValuesFromWhere(jsonQuery.getJSONArray("condition")));
		}
		
		String resultSql = String.format(sql.toString(), jsonQuery.has("fields")?fieldsWithoutQuotes(jsonQuery.getJSONArray("fields")):"*", tableName);
		
		List<Row> result = SqlMethods.select(resultSql, resultValues, connection);
		
		for(Row row : result) {
			row.removeCell("id");
			row.removeCell(playerId.getFieldName());
		}
		
		return result;
	}
	
	public static List<Row> select(Select select, PlayerId playerId, String tableNamePrefix, Connection connection) throws SQLException, JSONException {
		
		SqlExpression where = select.getWhere();
		SimpleSqlExpression playerIdWhereExpression = new SimpleSqlExpression(playerId.getFieldName(), playerId.getValue());
		
		if(where == null) {
			where = playerIdWhereExpression;
		} else {
			where = new SqlLogicExpression(playerIdWhereExpression, "AND", where);
		}
		
		select.setWhere(where);
		select.setTable(tableNamePrefix+select.getTable());
		
		List<Row> result = SqlMethods.select(select.toString(), where.getTypedValues(), connection);
		
		for(Row row : result) {
			row.removeCell("id");
			row.removeCell(playerId.getFieldName());
		}
		
		return result;
	}
	
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
	
	public static int createLifeRequest(String gamePrefix, String requestId, String requestedPlayerId, String needyPlayerId, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(requestId, requestedPlayerId, needyPlayerId);
		return SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES (?,?,?,'open')", typedValueArray.getQueryValues(), connection);
	}
	
	public static int createConfirmedLifeRequest(String gamePrefix, String requestId, String requestedPlayerId, String needyPlayerId, Connection connection) throws SQLException {
		
		//
		// This method create 'confirmed' life request, that to be able to send life directly to player, without opened request creation
		//
		
		TypedValueArray typedValueArray = new TypedValueArray(requestId, requestedPlayerId, needyPlayerId);
		return SqlMethods.insert("INSERT INTO "+gamePrefix+"life_requests (id, life_sender, life_receiver, status) VALUES (?,?,?,'confirmed')", typedValueArray.getQueryValues(), connection);
	}
	
	public static int confirmLifeRequest(String gamePrefix, String requestId, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(requestId);
		return SqlMethods.update("UPDATE "+gamePrefix+"life_requests SET status='confirmed' WHERE id=?", typedValueArray.getQueryValues(), connection);
	}
	
	public static int denyLifeRequest(String gamePrefix, String requestId, Connection connection) throws SQLException {
		return SqlMethods.delete("DELETE FROM "+gamePrefix+"life_requests WHERE id=?", new TypedValueArray(requestId).getQueryValues(), connection);
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
