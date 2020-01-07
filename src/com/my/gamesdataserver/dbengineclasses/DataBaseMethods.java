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
import com.my.gamesdataserver.basedbclasses.ColData;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Decrement;
import com.my.gamesdataserver.basedbclasses.Increment;
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
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.INTEGER, "ownerId", ownerId));
		row.add(new CellData(Types.VARCHAR, "api_key", apiKey));
		row.add(new CellData(Types.VARCHAR, "api_secret", apiSecret));
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
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.VARCHAR, "email", email));
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
		
		List<CellData> row = new ArrayList<>();
		
		row.add(new CellData("name", gameName));
		row.add(new CellData("package", gameJavaPackage));
		row.add(new CellData("owner_id", ownerId));
		row.add(new CellData("api_key", apiKey));
		row.add(new CellData("api_secret", apiSecret));
		row.add(new CellData("type", type));
		row.add(new CellData("prefix", prefix));
		row.add(new CellData("hash", hash));
		
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

	/*public boolean executeIncrement(Increment increment) throws SQLException, JSONException {
		return dataBaseInterface.executeIncrement(increment);
	}*/

	public static int executeDecrement(Decrement sqlRequest) {
		return 0;
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
			String gameName = (String)firstRow.get("name");
			String javaPackage = (String)firstRow.get("package");
			int ownerId = (int)firstRow.get("owner_id");
			String apiKey = (String)firstRow.get("api_key");
			String secretKey = (String)firstRow.get("api_secret");
			String type = (String)firstRow.get("type");
			String prefix = (String)firstRow.get("prefix");
			String hash = (String)firstRow.get("hash");
			game = new Game(gameName, javaPackage, ownerId, apiKey, secretKey, type, prefix, hash);
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
		List<CellData> row = new ArrayList<>();
		String playerId = generatePlayerId();
		row.add(new CellData("playerId", playerId));
		row.add(new CellData("facebookId", facebookId));
		row.add(new CellData("maxLevel", 0));
		if(SqlMethods.insertIntoTable(gamePrefix+"players", row, connection) <= 0) {
			return null;
		}
		return playerId;
	}
	
	private static String generatePlayerId() {
		return RandomKeyGenerator.nextString(8).toLowerCase()+"-"+RandomKeyGenerator.nextString(8).toLowerCase();
	}
}
