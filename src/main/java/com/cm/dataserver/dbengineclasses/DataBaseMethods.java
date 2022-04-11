package com.cm.dataserver.dbengineclasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.DataBaseConnectionParameters;
import com.cm.dataserver.DatabaseConnectionManager;
import com.cm.dataserver.basedbclasses.CellData;
import com.cm.dataserver.basedbclasses.Field;
import com.cm.dataserver.basedbclasses.QueryTypedValue;
import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.basedbclasses.SqlMethods;
import com.cm.dataserver.basedbclasses.TableIndex;
import com.cm.dataserver.basedbclasses.TableTemplate;
import com.cm.dataserver.basedbclasses.TypedValueArray;
import com.cm.dataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.cm.dataserver.helpers.RandomKeyGenerator;
import com.cm.dataserver.template1classes.Player;
import com.cm.dataserver.template1classes.PlayerMessage;

public class DataBaseMethods  {
	
	public static void createGameTables(GameTemplate gameTemplate, String prefix, Connection connection) throws SQLException {
		for(TableTemplate tableTemplate : gameTemplate.getTableTemplates()) {
			createGameTable(tableTemplate, prefix, connection);
		}
	}
	
	public static void createGameTable(TableTemplate table, String prefix, Connection connection) throws SQLException {
		SqlMethods.createTable(prefix, table, connection);
			
		for(TableIndex tIndex : table.getIndices()) {
			SqlMethods.createIndex(tIndex.getName(), prefix+table.getName(), tIndex.getFields(), tIndex.isUnique(), connection);
		}

		List<List<QueryTypedValue>> insertData = table.getDataForInsert();

		String sqlInsert = table.buildSqlInsert(prefix);

		for (List<QueryTypedValue> rowData : insertData) {
			SqlMethods.insert(sqlInsert, rowData, connection);
		}
	}
	
	public static int regOwner(String email, Connection connection) throws SQLException {
		return SqlMethods.insert("INSERT INTO owners (email) VALUES (?)", new QueryTypedValue(email), connection);
	}
	
	public static boolean checkGameByKey(String apiKey, Connection connection) throws SQLException {
		return SqlMethods.select("SELECT * FROM games WHERE api_key=?", new QueryTypedValue(apiKey), connection).size() > 0;
	}
	
	public static Game deleteGame(String apiKey, Connection connection) throws SQLException {
		Game game = getGameByKey(apiKey, connection);
		
		if(game != null) {
			//SqlMethods.deleteFrom("games", "api_key='"+apiKey+"'", connection);
			return game;
		}
		
		return null;
	}
	
	public static void deleteGameTables(String prefix, String[] templateNames, Connection connection) throws SQLException {
		/*for(String tableName : templateNames) {
			SqlMethods.dropTable(prefix+tableName, connection);
		}*/
	}
	
	public static int insertGame(String gameName, String gameType, int ownerId, String apiKey, String apiSecret, String prefix, String hash, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(gameName, ownerId, apiKey, apiSecret, gameType, prefix, hash);
		return SqlMethods.insert("INSERT INTO games (name, owner_id, api_key, api_secret, type, prefix, hash) VALUES (?,?,?,?,?,?,?)", typedValueArray.getQueryValues(), connection);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
	
	public static String[] getTablesNamesOfGame(String dataBaseName, String prefix, Connection connection) throws SQLException {
		return SqlMethods.findTablesByPrefix(dataBaseName, prefix, connection);
	}
	
	public static Game getGameByKey(String apiKey, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM games WHERE api_key=? LIMIT 1", new QueryTypedValue(apiKey), connection);
		return rowToGame(rows.get(0));
	}

	public static Game getGameByHash(String gameHash, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM games WHERE hash=? LIMIT 1", new QueryTypedValue(gameHash), connection);
		
		if(rows.size() < 1) {
			return null;
		}
		
		return rowToGame(rows.get(0));
	}
	
	private static Game rowToGame(Row row) throws SQLException {
		int id = row.containsCell("id")?(int)row.get("id"):-1;
		String gameName = row.containsCell("name")?(String)row.get("name"):null;
		String gameType = row.containsCell("type")?(String)row.get("type"):null;
		int ownerId = row.containsCell("owner_id")?(int)row.get("owner_id"):-1;
		String apiKey = row.containsCell("api_key")?(String)row.get("api_key"):null;
		String secretKey = row.containsCell("api_secret")?(String)row.get("api_secret"):null;
		String prefix = row.containsCell("prefix")?(String)row.get("prefix"):null;
		String hash = row.containsCell("hash")?(String)row.get("hash"):null;
		return new Game(id, gameName, gameType, ownerId, apiKey, secretKey, prefix, hash);
	}

	public static Player getPlayerById(PlayerId playerId, String gamePrefix, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM "+gamePrefix+"players WHERE "+playerId.getFieldName()+"=? LIMIT 1", new QueryTypedValue(playerId.getValue()), connection);
		return rowToPlayer(rows.get(0), connection);
	}
	
	public static Player getPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM "+gamePrefix+"players WHERE facebookId=? LIMIT 1", new QueryTypedValue(facebookId), connection);
		
		if(rows == null || rows.size() <= 0) {
			return null;
		}
		
		return rowToPlayer(rows.get(0), connection);
	}
	
	private static Player rowToPlayer(Row row, Connection connection) throws SQLException {
		return new Player((String)row.get("playerId"), (String)row.get("facebookId"));
	}
	
	public static String registrationPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		String playerId = generatePlayerId();
		
		if(SqlMethods.insert("INSERT INTO "+gamePrefix+"players VALUES (?,?)", new TypedValueArray(playerId, facebookId).getQueryValues(), connection) <= 0) {
			return null;
		}
		
		return playerId;
	}
	
	private static String generatePlayerId() {
		return RandomKeyGenerator.nextString(8).toLowerCase()+"-"+RandomKeyGenerator.nextString(8).toLowerCase();
	}
	
	public static List<SpecialRequest> readSpecialRequests(int gameId, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM special_requests WHERE game_id=?", new QueryTypedValue(gameId), connection);
		
		List<SpecialRequest> result = new ArrayList<>();
		
		for(Row row : rows) {
			result.add(rowToSpecialRequest(row, connection));
		}
			
		return result;
	}
	
	private static SpecialRequest rowToSpecialRequest(Row row, Connection connection) throws SQLException {
		int fetchedGameId = (int) row.get("game_id");
		String requestName = (String) row.get("request_name");
		String table = (String) row.get("query_table");
		String fields = (String) row.get("fields");
		return new SpecialRequest(fetchedGameId, requestName, table, fields);
	}
	
	public static SpecialRequest getSpecialRequestByName(int gameId, String requestName, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM special_requests WHERE game_id=? AND request_name=? LIMIT 1", new TypedValueArray(gameId, requestName).getQueryValues(), connection);
		
		SpecialRequest result = null;
		
		if(rows.size() > 0) {
			result = rowToSpecialRequest(rows.get(0), connection);
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
	
	public static int addSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		return SqlMethods.insert("INSERT INTO special_requests VALUES (?,?,?,?)", new TypedValueArray(gameId, requestName, table, fields).getQueryValues(), connection);
	}
	
	public static List<Row> executeSpecialRequest(int gameId, String specialRequestName, String tablePrefix, JSONArray where, Connection connection) throws JSONException, SQLException {
		SpecialRequest specialRequest = getSpecialRequestByName(gameId, specialRequestName, connection);
		
		if(specialRequest == null) return new ArrayList<Row>();
		
		specialRequest.setTable(tablePrefix+specialRequest.getTable());
		specialRequest.setWhere(where);
		return SqlMethods.select(specialRequest.toString(), getValuesFromWhere(where), connection);
	}

	public static Owner getOwnerByEmail(String email, Connection dbConnection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM owners WHERE email=? LIMIT 1", new QueryTypedValue(email), dbConnection);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		return new Owner((int) rows.get(0).get("id"), (String) rows.get(0).get("email"));
	}
	
	public static List<String> getAllGamePrefixes(Connection dbConnection) throws SQLException {
		List<String> result = new ArrayList<>();
		List<Row> rows = SqlMethods.select("SELECT prefix FROM games", dbConnection);
		
		for (Row row : rows) {
			result.add(row.getString("prefix"));
		}
		
		return result;
	}
	
	public static List<Game> getAllGames(Connection dbConnection) throws SQLException {
		List<Game> result = new ArrayList<>();
		List<Row> rows = SqlMethods.select("SELECT id,prefix FROM games", dbConnection);
		
		for (Row row : rows) {
			result.add(rowToGame(row));
		}
		
		return result;
	}
	
	public static List<Game> getOwnerGames(Connection dbConnection, String[] fields, String ownerEmail) throws SQLException {
		List<Game> result = new ArrayList<>();
		List<Row> owners = SqlMethods.select("SELECT id FROM owners WHERE email=? LIMIT 1", new QueryTypedValue(ownerEmail), dbConnection);
		
		if(owners.size() <= 0) return result;
		
		String queryFields = "*";
		
		if(fields != null && fields.length > 0) {
			queryFields = String.join(",", fields);
		}
		
		int ownerId = owners.get(0).getInt("id");
		List<Row> games = SqlMethods.select("SELECT "+queryFields+" FROM games WHERE owner_id=?", new QueryTypedValue(ownerId), dbConnection);
		
		for (Row row : games) {
			result.add(rowToGame(row));
		}
		
		return result;
	}
	
	/*public static int serverTypeToMysqType(String serverType) {
		switch (serverType) {
		case "integer":
			return Types.INTEGER;
		case "string":
			return Types.VARCHAR;
		case "float":
			return Types.FLOAT;
		}
		return Types.NULL;
	}*/
}
