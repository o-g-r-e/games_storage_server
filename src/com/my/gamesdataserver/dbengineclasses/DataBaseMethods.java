package com.my.gamesdataserver.dbengineclasses;

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

import org.json.JSONException;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.DatabaseConnectionManager;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Field;
import com.my.gamesdataserver.basedbclasses.QueryTypedValue;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.TableIndex;
import com.my.gamesdataserver.basedbclasses.TableTemplate;
import com.my.gamesdataserver.basedbclasses.TypedValueArray;
import com.my.gamesdataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;

public class DataBaseMethods  {
	
	public static void createGameTables(GameTemplate gameTemplate, String prefix, Connection connection) throws SQLException {
		for(TableTemplate tableTemplate : gameTemplate.getTableTemplates()) {
			createGameTable(tableTemplate, prefix, connection);
		}
	}
	
	public static void createGameTable(TableTemplate table, String prefix, Connection connection) throws SQLException {
		SqlMethods.createTable(prefix+table.getName(), table.getCols(), table.getPrimaryKey(), connection);
			
		for(TableIndex tIndex : table.getIndices()) {
			SqlMethods.createIndex(tIndex.getName(), prefix+table.getName(), tIndex.getFields(), tIndex.isUnique(), connection);
		}
	}
	
	public static int regOwner(String email, Connection connection) throws SQLException {
		return SqlMethods.insert("INSERT INTO owners VALUES (?)", new QueryTypedValue(email), connection);
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
	
	public static int insertGame(String gameName, int ownerId, String apiKey, String apiSecret, String prefix, String hash, Connection connection) throws SQLException {
		TypedValueArray typedValueArray = new TypedValueArray(gameName, ownerId, apiKey, apiSecret, prefix, hash);

		
		return SqlMethods.insert("INSERT INTO games (name, owner_id, api_key, api_secret, prefix, hash) VALUES (?,?,?,?,?,?)", typedValueArray.getQueryValues(), connection);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
	
	public static String[] getTablesNamesOfGame(String dataBaseName, String prefix, Connection connection) throws SQLException {
		return SqlMethods.findTablesByPrefix(dataBaseName, prefix, connection);
	}
	
	public static Game getGameByKey(String apiKey, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM games WHERE api_key=? LIMIT 1", new QueryTypedValue(apiKey), connection);
		return rowToGame(rows.get(0), connection);
	}

	public static Game getGameByHash(String gameHash, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM games WHERE hash=? LIMIT 1", new QueryTypedValue(gameHash), connection);
		return rowToGame(rows.get(0), connection);
	}
	
	private static Game rowToGame(Row row, Connection connection) throws SQLException {
		int id = (int) row.get("id");
		String gameName = (String)row.get("name");
		int ownerId = (int)row.get("owner_id");
		String apiKey = (String)row.get("api_key");
		String secretKey = (String)row.get("api_secret");
		String prefix = (String)row.get("prefix");
		String hash = (String)row.get("hash");
		return new Game(id, gameName, ownerId, apiKey, secretKey, prefix, hash);
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
	
	public static int addSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		return SqlMethods.insert("INSERT INTO special_requests VALUES (?,?,?,?)", new TypedValueArray(gameId, requestName, table, fields).getQueryValues(), connection);
	}
	
	public static List<Row> executeSpecialRequest(int gameId, String specialRequestName, String tablePrefix, Connection connection) throws JSONException, SQLException {
		SpecialRequest specialRequest = getSpecialRequestByName(gameId, specialRequestName, connection);
		
		if(specialRequest == null) {
			return new ArrayList<Row>();
		}
		
		return SqlMethods.select("SELECT "+specialRequest.getFields()+" FROM "+specialRequest.getTable(), connection);
	}

	public static Owner getOwnerByEmail(String email, Connection dbConnection) throws SQLException {
		List<Row> rows = SqlMethods.select("SELECT * FROM owners WHERE email=? LIMIT 1", new QueryTypedValue(email), dbConnection);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		return new Owner((int) rows.get(0).get("id"), (String) rows.get(0).get("email"));
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
