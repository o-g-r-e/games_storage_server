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
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("owners", "email="+email);
		
		if(rows.size() > 0) {
			owner = new Owner((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue());
		}
		
		return owner;
	}
	
	public String getOwnerEmailById(int id) throws SQLException {
		Owner owner = null;
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("owners", "id="+id);
		
		if(rows.size() > 0) {
			owner = new Owner((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue());
		}
		String result = owner.getEmail();
		return result;
	}
	
	public boolean checkGameByKey(String apiKey) throws SQLException {
		
		return dataBaseInterface.selectAllWhere("games", "api_key="+apiKey).size() > 0;
	}
	
	public Game getGameByKey(String apiKey) throws SQLException {
		Game game = null;
		
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "api_key", apiKey));
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("games", where);
		
		if(rows.size() > 0) {
			String gameName = (String)rows.get(0).get(0).getValue();
			String javaPackage = (String)rows.get(0).get(1).getValue();
			int ownerId = (int)   rows.get(0).get(2).getValue();
			//String apiKey = (String)rows.get(0).get(3).getValue();
			String secretKey = (String)rows.get(0).get(4).getValue();
			String type = (String)rows.get(0).get(5).getValue();
			String prefix = (String)rows.get(0).get(6).getValue();
			String hash = (String)rows.get(0).get(7).getValue();
			game = new Game(gameName, javaPackage, ownerId, apiKey, secretKey, type, prefix, hash);
		}
		
		return game;
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
		
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("owner_secrets", "api_key="+apiKey);
		
		if(rows.size() > 0) {
			result = new OwnerSecrets((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (String)rows.get(0).get(2).getValue());
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

	public List<List<CellData>> executeSelect(SqlSelect sqlRequest) throws SQLException {
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

	public boolean executeIncrement(Increment increment) throws SQLException, JSONException {
		return dataBaseInterface.executeIncrement(increment);
	}

	public int executeDecrement(Decrement sqlRequest) {
		return 0;
	}

	public Game getGameByHash(String gameHash) throws SQLException {
		Game game = null;
		List<SqlExpression> where = new ArrayList<>();
		where.add(new SqlExpression(Types.VARCHAR, "hash", gameHash));
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("games", where);
		
		if(rows.size() > 0) {
			String gameName = (String)rows.get(0).get(0).getValue();
			String javaPackage = (String)rows.get(0).get(1).getValue();
			int ownerId = (int)   rows.get(0).get(2).getValue();
			String apiKey = (String)rows.get(0).get(3).getValue();
			String secretKey = (String)rows.get(0).get(4).getValue();
			String type = (String)rows.get(0).get(5).getValue();
			String prefix = (String)rows.get(0).get(6).getValue();
			String hash = (String)rows.get(0).get(7).getValue();
			game = new Game(gameName, javaPackage, ownerId, apiKey, secretKey, type, prefix, hash);
		}
		
		return game;
	}

	public Player getPlayerById(String playerId, String gamePrefix) throws SQLException {
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(gamePrefix+"players", "playerId="+playerId);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		return new Player((String)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (int)rows.get(0).get(2).getValue());
	}
	
	public Player getPlayerByFacebookId(String facebookId, String gamePrefix) throws SQLException {
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(gamePrefix+"players", "facebookId="+facebookId);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		return new Player((String)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (int)rows.get(0).get(2).getValue());
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
