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
import com.my.gamesdataserver.SqlExpression;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Field;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;

public class DataBaseMethods  {
	
	public static void createGameTables(GameTemplate gameTemplate, String prefix, Connection connection) throws SQLException {
		for(TableTemplate tableTemplate : gameTemplate.getTableTemplates()) {
			
			SqlMethods.createTable(prefix+tableTemplate.getName(), tableTemplate.getCols(), tableTemplate.getPrimaryKey(), connection);
			
			for(TableIndex tIndex : tableTemplate.getIndices()) {
				SqlMethods.createIndex(tIndex.getName(), prefix+tableTemplate.getName(), tIndex.getFields(), tIndex.isUnique(), connection);
			}
		}
	}
	
	public static int writeNewOwnerSecrets(int ownerId, String apiKey, String apiSecret, Connection connection) throws SQLException {
		List<SqlExpression> row = new ArrayList<>();
		row.add(new SqlExpression(Types.INTEGER, "ownerId", ownerId));
		row.add(new SqlExpression(Types.VARCHAR, "api_key", apiKey));
		row.add(new SqlExpression(Types.VARCHAR, "api_secret", apiSecret));
		return SqlMethods.insertIntoTable("owner_secrets", row, connection);
	}
	
	public static int updateGame(String name, String gameJavaPackage) {
		return 0;
	}
	
	/*public int preRegOwner(String newOwnerName, String activationId) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.VARCHAR, "activation_id", activationId));
		row.add(new CellData(Types.VARCHAR, "new_owner_name", newOwnerName));
		return insertIntoTable("pre_reg_owners", row);
	}*/
	
	public static int regOwner(String email, Connection connection) throws SQLException {
		List<SqlExpression> row = new ArrayList<>();
		row.add(new SqlExpression(Types.VARCHAR, "email", email));
		return SqlMethods.insertIntoTable("owners", row, connection);
	}
	
	public static Owner getOwnerByEmail(String email, Connection connection) throws SQLException {
		Owner owner = null;
		List<Row> rows = SqlMethods.selectAll("owners", "email="+email, connection);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			owner = new Owner((int)firstRow.get("id"), (String)firstRow.get("email"));
		}
		
		return owner;
	}
	
	public static String getOwnerEmailById(int id, Connection connection) throws SQLException {
		Owner owner = null;
		List<Row> rows = SqlMethods.selectAll("owners", "id="+id, connection);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			owner = new Owner((int)firstRow.get("id"), (String)firstRow.get("email"));
		}
		String result = owner.getEmail();
		return result;
	}
	
	public static boolean checkGameByKey(String apiKey, Connection connection) throws SQLException {
		
		return SqlMethods.selectAll("games", "api_key="+apiKey, connection).size() > 0;
	}
	
	public static Game deleteGame(String apiKey, Connection connection) throws SQLException {
		Game game = getGameByKey(apiKey, connection);
		
		if(game != null) {
			SqlMethods.deleteFrom("games", "api_key='"+apiKey+"'", connection);
			return game;
		}
		
		return null;
	}
	
	public static void deleteGameTables(String prefix, String[] templateNames, Connection connection) throws SQLException {
		for(String tableName : templateNames) {
			SqlMethods.dropTable(prefix+tableName, connection);
		}
	}

	public static OwnerSecrets getOwnerSecrets(String apiKey, Connection connection) throws SQLException {
		OwnerSecrets result = null;
		
		List<Row> rows = SqlMethods.selectAll("owner_secrets", "api_key="+apiKey, connection);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			result = new OwnerSecrets((int)firstRow.get("ownerId"), (String)firstRow.get("api_key"), (String)firstRow.get("api_secret"));
		}
		
		return result;
	}
	
	public static void removeApiKey(String apiKey, Connection connection) throws SQLException {
		SqlMethods.deleteFrom("owner_secrets", "api_key='"+apiKey+"'", connection);
	}
	
	public static int insertGame(String gameName, String gameJavaPackage, int ownerId, String apiKey, String apiSecret, String type, String prefix, String hash, Connection connection) throws SQLException {
		
		List<SqlExpression> row = new ArrayList<>();
		
		row.add(new SqlExpression("name", gameName));
		row.add(new SqlExpression("package", gameJavaPackage));
		row.add(new SqlExpression("owner_id", ownerId));
		row.add(new SqlExpression("api_key", apiKey));
		row.add(new SqlExpression("api_secret", apiSecret));
		row.add(new SqlExpression("type", type));
		row.add(new SqlExpression("prefix", prefix));
		row.add(new SqlExpression("hash", hash));
		
		return SqlMethods.insertIntoTable("games", row, connection);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
	
	public static String[] getTablesNamesOfGame(String dataBaseName, String prefix, Connection connection) throws SQLException {
		return SqlMethods.findTablesByPrefix(dataBaseName, prefix, connection);
	}

	/*public boolean isTransactionsEnabled() throws SQLException {
		return dataBaseInterface.isTransactionsEnabled();
	}

	public void rollback() throws SQLException {
		dataBaseInterface.rollback();
	}

	public void enableTransactions() throws SQLException {
		dataBaseInterface.enableTransactions();
	}

	public void commit() throws SQLException {
		dataBaseInterface.commit();
	}

	public void disableTransactions() throws SQLException {
		dataBaseInterface.disableTransactions();
	}*/

	public static List<Row> executeSelect(SqlSelect sqlRequest, Connection connection) throws SQLException {
		return SqlMethods.executeSelect(sqlRequest, connection);
	}

	public static int executeInsert(SqlInsert sqlRequest, Connection connection) throws SQLException {
		
		if(sqlRequest.getRowToInsert().size() > 0) {
			return SqlMethods.executeInsert(sqlRequest, connection);
		}
		
		return 0;
	}

	public static int executeUpdate(SqlUpdate sqlRequest, Connection connection) throws SQLException {
		return SqlMethods.executeUpdate(sqlRequest, connection);
	}
	
	public static Game getGameByKey(String apiKey, Connection connection) throws SQLException {
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "api_key", apiKey));
		return getGame(where, connection);
	}

	public static Game getGameByHash(String gameHash, Connection connection) throws SQLException {
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "hash", gameHash));
		return getGame(where, connection);
	}
	
	private static Game getGame(List<SqlExpression> where, Connection connection) throws SQLException {
		Game game = null;
		List<Row> rows = SqlMethods.selectAll("games", where, connection);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			int id = (int) firstRow.get("id");
			String gameName = (String)firstRow.get("name");
			String javaPackage = (String)firstRow.get("package");
			int ownerId = (int)firstRow.get("owner_id");
			String apiKey = (String)firstRow.get("api_key");
			String secretKey = (String)firstRow.get("api_secret");
			String type = (String)firstRow.get("type");
			String prefix = (String)firstRow.get("prefix");
			String hash = (String)firstRow.get("hash");
			game = new Game(id, gameName, javaPackage, ownerId, apiKey, secretKey, type, prefix, hash);
		}
		
		return game;
	}

	public static Player getPlayerById(String playerId, String gamePrefix, Connection connection) throws SQLException {
		return getPlayer(gamePrefix+"players", "playerId="+playerId, connection);
	}
	
	public static Player getPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		return getPlayer(gamePrefix+"players", "facebookId="+facebookId, connection);
	}
	
	private static Player getPlayer(String tableName, String expression, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.selectAll(tableName, expression, connection);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		Row firstRow = rows.get(0);
		
		return new Player((String)firstRow.get("playerId"), (String)firstRow.get("facebookId"), (int)firstRow.get("maxLevel"));
	}
	
	public static String registrationPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		List<SqlExpression> row = new ArrayList<>();
		String playerId = generatePlayerId();
		row.add(new SqlExpression("playerId", playerId));
		row.add(new SqlExpression("facebookId", facebookId));
		row.add(new SqlExpression("maxLevel", 0));
		if(SqlMethods.insertIntoTable(gamePrefix+"players", row, connection) <= 0) {
			return null;
		}
		return playerId;
	}
	
	private static String generatePlayerId() {
		return RandomKeyGenerator.nextString(8).toLowerCase()+"-"+RandomKeyGenerator.nextString(8).toLowerCase();
	}
	
	public static List<SpecialRequest> readSpecialRequests(int gameId, Connection connection) throws SQLException {
		List<Row> resultRows = SqlMethods.selectAll("special_requests", "game_id="+gameId, connection);
		
		List<SpecialRequest> result = new ArrayList<>();
		
		for(Row row : resultRows) {
			int fetchedGameId = (int) row.get("game_id");
			String requestName = (String) row.get("request_name");
			String table = (String) row.get("query_table");
			String fields = (String) row.get("fields");
			result.add(new SpecialRequest(fetchedGameId, requestName, table, fields));
		}
			
		return result;
	}
	
	public static SpecialRequest readSpecialRequest(int gameId, String requestName, Connection connection) throws SQLException {
		List<Row> resultRows = SqlMethods.selectAll("special_requests", "game_id="+gameId+"&request_name="+requestName, connection);
		
		SpecialRequest result = null;
		
		if(resultRows.size() > 0) {
			int fetchedGameId = (int) resultRows.get(0).get("game_id");
			String fetchedRequestName = (String) resultRows.get(0).get("request_name");
			String table = (String) resultRows.get(0).get("query_table");
			String fields = (String) resultRows.get(0).get("fields");
			result = new SpecialRequest(fetchedGameId, fetchedRequestName, table, fields);
		}
			
		return result;
	}
	
	public static int setSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		
		String whereLiteral = "game_id="+gameId+"&request_name="+requestName;
		String setClause = "game_id="+gameId+"&request_name="+requestName+"&query_table="+table+"&fields="+fields;
		
		if(SqlMethods.selectAll("special_requests", whereLiteral, connection).size() > 0) {
			return SqlMethods.updateTable("special_requests", setClause, whereLiteral, connection);
		}
		return SqlMethods.insertIntoTable("special_requests", setClause, connection);
	}
	
	public static int addSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		String setClause = "request_name="+requestName+"&query_table="+table+"&fields="+fields;
		return SqlMethods.insertIntoTable("special_requests", setClause, connection);
	}
	
	public static void updateSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		String whereLiteral = "game_id="+gameId+"&request_name="+requestName;
		String setClause = "request_name="+requestName+"&query_table="+table+"&fields="+fields;
		SqlMethods.updateTable("special_requests", setClause, whereLiteral, connection);
	}
	
	public static List<Row> executeSpecialRequest(SpecialRequest specialRequest, List<SqlExpression> whereCondition, String tablePrefix, Connection connection) throws JSONException, SQLException {
		SqlSelect s = new SqlSelect(tablePrefix+specialRequest.getTable(), whereCondition);
		s.setFields(specialRequest.getFieldsList());
		return SqlMethods.executeSelect(s, connection);
	}
}
