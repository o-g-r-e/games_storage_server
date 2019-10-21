package com.my.gamesdataserver.gamesdbmanager;

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

import com.my.gamesdataserver.ClientHandler;
import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.GameTemplate;
import com.my.gamesdataserver.TableTemplate;
import com.my.gamesdataserver.rawdbmanager.CellData;
import com.my.gamesdataserver.rawdbmanager.ColData;
import com.my.gamesdataserver.rawdbmanager.DataBaseInterface;

public class DatabaseEngine extends DataBaseInterface {
	
	private Map<String, Set<String>> tablesData = new HashMap<>(); //<game api key, coressponding tables names>
	
	public Map<String, Set<String>> getTablesData() {
		return tablesData;
	}

	public DatabaseEngine(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		super(dbConnectionParameters);
	}
	
	/*public void createTableSet(String prefix, String[] match3TableNameSet) throws SQLException {
		ColData[] scoreLevelCols = {new ColData(Types.INTEGER, "playerId"),
									new ColData(Types.INTEGER, "level"),
									new ColData(Types.INTEGER, "score"),
									new ColData(Types.INTEGER, "stars")};
		createTable(prefix+match3TableNameSet[0], scoreLevelCols);
		
		ColData[] playerslCols = {new ColData(Types.INTEGER, "playerId"), new ColData(Types.INTEGER, "max_level")};
		createTable(prefix+match3TableNameSet[1], playerslCols);
		
		ColData[] boostsCols = {new ColData(Types.INTEGER, "playerId"), new ColData(Types.VARCHAR, "name"), new ColData(Types.INTEGER, "count")};
		createTable(prefix+match3TableNameSet[2], boostsCols);
	}*/
	
	public void createGameTables(GameTemplate gameTemplate, String prefix) throws SQLException {
		for(TableTemplate tt : gameTemplate.getTables()) {
			createTable(prefix+tt.getName(), tt.getCols());
		}
	}
	
	public int writeNewApiKey(String ownerEmail, String apiKey) throws SQLException {
		List<CellData> row = new ArrayList<>();
		int ownerId = getOwnerByEmail(ownerEmail).getId();
		
		if(ownerId > 0) {
			row.add(new CellData(Types.VARCHAR, "owner_id", ownerId));
			row.add(new CellData(Types.VARCHAR, "api_key", apiKey));
			return insertIntoTable("api_keys", row);
		}
		
		return 0;
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
		return insertIntoTable("owners", row);
	}
	
	public boolean checkOwnerByEmail(String ownerEmail) throws SQLException {
		return selectAllWhere("owners", "email="+ownerEmail).size() > 0;
	}
	
	public Owner getOwnerByEmail(String email) throws SQLException {
		Owner owner = null;
		List<List<CellData>> rows = selectAllWhere("owners", "email="+email);
		
		if(rows.size() > 0) {
			owner = new Owner((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue());
		}
		
		return owner;
	}
	public boolean checkGameByKey(String apiKey) throws SQLException {
		
		if(tablesData.containsKey(apiKey)) {
			return true;
		}
		
		return selectAllWhere("games", "api_key="+apiKey).size() > 0;
	}
	
	public Game getGameByKey(String apiKey) throws SQLException {
		Game game = null;
		List<List<CellData>> rows = selectAllWhere("games", "api_key="+apiKey);
		
		if(rows.size() > 0) {
			game = new Game((int)rows.get(0).get(0).getValue(), 
							(String)rows.get(0).get(1).getValue(), 
							(String)rows.get(0).get(2).getValue(), 
							(int)   rows.get(0).get(3).getValue(), 
							(String)rows.get(0).get(4).getValue(), 
							(String)rows.get(0).get(5).getValue(), 
							(String)rows.get(0).get(6).getValue());
		}
		
		return game;
	}
	
	public Game deleteGame(String apiKey) throws SQLException {
		Game game = getGameByKey(apiKey);
		
		if(game != null) {
			deleteFrom("games", "api_key='"+apiKey+"'");
			return game;
		}
		
		return null;
	}
	
	public void deleteGameTables(String prefix, String[] templateNames) throws SQLException {
		for(String tableName : templateNames) {
			dropTable(prefix+tableName);
		}
	}

	public ApiKey getApiKey(String apiKey) throws SQLException {
		ApiKey result = null;
		
		List<List<CellData>> rows = selectAllWhere("api_keys", "api_key="+apiKey);
		
		if(rows.size() > 0) {
			result = new ApiKey((int)rows.get(0).get(0).getValue(), (int)rows.get(0).get(1).getValue(), (String)rows.get(0).get(2).getValue());
		}
		
		return result;
	}
	
	public void removeApiKey(String apiKey) throws SQLException {
		deleteFrom("api_keys", "api_key='"+apiKey+"'");
	}
	
	List<Game> selectGames() throws SQLException {
		List<Game> result = new ArrayList<Game>();
		
		List<List<CellData>> rows = selectAll("games");
		for(List<CellData> row : rows) {
			result.add(new Game((int)row.get(0).getValue(), 
					(String)row.get(1).getValue(), 
					(String)row.get(2).getValue(),
					(int)   row.get(3).getValue(), 
					(String)row.get(4).getValue(), 
					(String)row.get(5).getValue(), 
					(String)row.get(6).getValue()));
		}
		return result;
	}
	
	public int insertGame(String gameName, String gameJavaPackage, int ownerId, String key, String type, String prefix) throws SQLException {
		PreparedStatement pstmt = getCon().prepareStatement("INSERT INTO games (`name`, `package`, `owner_id`, `api_key`, `type`, `prefix`) VALUES (?, ?, ?, ?, ?, ?)");
		pstmt.setString(1, gameName);
		pstmt.setString(2, gameJavaPackage);
		pstmt.setInt(3, ownerId);
		pstmt.setString(4, key);
		pstmt.setString(5, type);
		pstmt.setString(6, prefix);
		return pstmt.executeUpdate();
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
}
