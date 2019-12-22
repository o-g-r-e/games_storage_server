package com.my.gamesdataserver.dbengineclasses;

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
import com.my.gamesdataserver.SqlExpression;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.ColData;
import com.my.gamesdataserver.basedbclasses.DataBaseInterface;
import com.my.gamesdataserver.basedbclasses.Decrement;
import com.my.gamesdataserver.basedbclasses.Increment;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;

public class GamesDbEngine  {
	
	private DataBaseInterface dataBaseInterface;

	public GamesDbEngine(DataBaseInterface dataBaseInterface) {
		this.dataBaseInterface = dataBaseInterface;
	}
	
	public void createGameTables(GameTemplate gameTemplate, String prefix) throws SQLException {
		
		for(TableTemplate tableTemplate : gameTemplate.getTableTemplates()) {
			
			dataBaseInterface.createTable(prefix+tableTemplate.getName(), tableTemplate.getCols(), tableTemplate.getPrimaryKey());
			
			for(TableIndex tIndex : tableTemplate.getIndices()) {
				dataBaseInterface.createIndex(tIndex.getName(), prefix+tableTemplate.getName(), tIndex.getFields(), tIndex.isUnique());
			}
		}
	}
	
	public int writeNewOwnerSecrets(int ownerId, String apiKey, String apiSecret) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.INTEGER, "ownerId", ownerId));
		row.add(new CellData(Types.VARCHAR, "api_key", apiKey));
		row.add(new CellData(Types.VARCHAR, "api_secret", apiSecret));
		return dataBaseInterface.insertIntoTable("owner_secrets", row);
	}
	
	public int updateGame(String name, String gameJavaPackage) {
		return 0;
	}
	
	/*public int preRegOwner(String newOwnerName, String activationId) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.VARCHAR, "activation_id", activationId));
		row.add(new CellData(Types.VARCHAR, "new_owner_name", newOwnerName));
		return insertIntoTable("pre_reg_owners", row);
	}*/
	
	public int regOwner(String email) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.VARCHAR, "email", email));
		return dataBaseInterface.insertIntoTable("owners", row);
	}
	
	public Owner getOwnerByEmail(String email) throws SQLException {
		Owner owner = null;
		List<Row> rows = dataBaseInterface.selectAll("owners", "email="+email);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			owner = new Owner((int)firstRow.get("id"), (String)firstRow.get("email"));
		}
		
		return owner;
	}
	
	public String getOwnerEmailById(int id) throws SQLException {
		Owner owner = null;
		List<Row> rows = dataBaseInterface.selectAll("owners", "id="+id);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			owner = new Owner((int)firstRow.get("id"), (String)firstRow.get("email"));
		}
		String result = owner.getEmail();
		return result;
	}
	
	public boolean checkGameByKey(String apiKey) throws SQLException {
		
		return dataBaseInterface.selectAll("games", "api_key="+apiKey).size() > 0;
	}
	
	public Game deleteGame(String apiKey) throws SQLException {
		Game game = getGameByKey(apiKey);
		
		if(game != null) {
			dataBaseInterface.deleteFrom("games", "api_key='"+apiKey+"'");
			return game;
		}
		
		return null;
	}
	
	public void deleteGameTables(String prefix, String[] templateNames) throws SQLException {
		for(String tableName : templateNames) {
			dataBaseInterface.dropTable(prefix+tableName);
		}
	}

	public OwnerSecrets getOwnerSecrets(String apiKey) throws SQLException {
		OwnerSecrets result = null;
		
		List<Row> rows = dataBaseInterface.selectAll("owner_secrets", "api_key="+apiKey);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			result = new OwnerSecrets((int)firstRow.get("ownerId"), (String)firstRow.get("api_key"), (String)firstRow.get("api_secret"));
		}
		
		return result;
	}
	
	public void removeApiKey(String apiKey) throws SQLException {
		dataBaseInterface.deleteFrom("owner_secrets", "api_key='"+apiKey+"'");
	}
	
	public int insertGame(String gameName, String gameJavaPackage, int ownerId, String apiKey, String apiSecret, String type, String prefix, String hash) throws SQLException {
		
		List<CellData> row = new ArrayList<>();
		
		row.add(new CellData("name", gameName));
		row.add(new CellData("package", gameJavaPackage));
		row.add(new CellData("owner_id", ownerId));
		row.add(new CellData("api_key", apiKey));
		row.add(new CellData("api_secret", apiSecret));
		row.add(new CellData("type", type));
		row.add(new CellData("prefix", prefix));
		row.add(new CellData("hash", hash));
		
		return dataBaseInterface.insertIntoTable("games", row);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
	
	public String[] getTablesNamesOfGame(String prefix) throws SQLException {
		return dataBaseInterface.findTablesByPrefix(prefix);
	}

	public boolean isTransactionsEnabled() throws SQLException {
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
	}

	public List<Row> executeSelect(SqlSelect sqlRequest) throws SQLException {
		return dataBaseInterface.executeSelect(sqlRequest);
	}

	public int executeInsert(SqlInsert sqlRequest) throws SQLException {
		
		if(sqlRequest.getRowToInsert().size() > 0) {
			return dataBaseInterface.executeInsert(sqlRequest);
		}
		
		return 0;
	}

	public int executeUpdate(SqlUpdate sqlRequest) throws SQLException {
		return dataBaseInterface.executeUpdate(sqlRequest);
	}

	/*public boolean executeIncrement(Increment increment) throws SQLException, JSONException {
		return dataBaseInterface.executeIncrement(increment);
	}*/

	public int executeDecrement(Decrement sqlRequest) {
		return 0;
	}
	
	public Game getGameByKey(String apiKey) throws SQLException {
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "api_key", apiKey));
		return getGame(where);
	}

	public Game getGameByHash(String gameHash) throws SQLException {
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "hash", gameHash));
		return getGame(where);
	}
	
	private Game getGame(List<SqlExpression> where) throws SQLException {
		Game game = null;
		List<Row> rows = dataBaseInterface.selectAll("games", where);
		
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

	public Player getPlayerById(String playerId, String gamePrefix) throws SQLException {
		return getPlayer(gamePrefix+"players", "playerId="+playerId);
	}
	
	public Player getPlayerByFacebookId(String facebookId, String gamePrefix) throws SQLException {
		return getPlayer(gamePrefix+"players", "facebookId="+facebookId);
	}
	
	private Player getPlayer(String tableName, String expression) throws SQLException {
		List<Row> rows = dataBaseInterface.selectAll(tableName, expression);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		Row firstRow = rows.get(0);
		
		return new Player((String)firstRow.get("playerId"), (String)firstRow.get("facebookId"), (int)firstRow.get("maxLevel"));
	}
	
	public String registrationPlayerByFacebookId(String facebookId, String gamePrefix) throws SQLException {
		List<CellData> row = new ArrayList<>();
		String playerId = generatePlayerId();
		row.add(new CellData("playerId", playerId));
		row.add(new CellData("facebookId", facebookId));
		row.add(new CellData("maxLevel", 0));
		if(dataBaseInterface.insertIntoTable(gamePrefix+"players", row) <= 0) {
			return null;
		}
		return playerId;
	}
	
	private String generatePlayerId() {
		return RandomKeyGenerator.nextString(8).toLowerCase()+"-"+RandomKeyGenerator.nextString(8).toLowerCase();
	}
}
