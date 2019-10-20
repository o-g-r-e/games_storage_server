package com.my.gamesdataserver.gamesdbmanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.rawdbmanager.CellData;
import com.my.gamesdataserver.rawdbmanager.ColData;
import com.my.gamesdataserver.rawdbmanager.DataBaseInterface;

public class GamesDbManager extends DataBaseInterface {
	
	private Map<String, Set<String>> tablesData = new HashMap<>(); //<game api key, coressponding tables names>
	
	public Map<String, Set<String>> getTablesData() {
		return tablesData;
	}

	public GamesDbManager(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		super(dbConnectionParameters);
		readTablesData();
	}
	
	private void readTablesData() throws SQLException {
		Map<String, Set<String>> result = new HashMap<>();
		List<GameEntity> games = selectGames();
		for(GameEntity g : games) {
			Set<String> set = new HashSet<>(Arrays.asList(g.getTables().split(",")));
			tablesData.put(g.getKey(), set);
		}
	}
	
	
	public void createMatch3TableSet(String prefix) throws SQLException {
		ColData[] scoreLevelCols = {new ColData(Types.INTEGER, "playerId"),
									new ColData(Types.INTEGER, "level"),
									new ColData(Types.INTEGER, "score"),
									new ColData(Types.INTEGER, "stars")};
		createTable(prefix+"ScoreLevel", scoreLevelCols);
		
		ColData[] playerslCols = {new ColData(Types.INTEGER, "playerId")};
		createTable(prefix+"Players", playerslCols);
		
		ColData[] boostsCols = {new ColData(Types.INTEGER, "playerId"), new ColData(Types.VARCHAR, "name"), new ColData(Types.INTEGER, "count")};
		createTable(prefix+"Boosts", boostsCols);
	}
	
	/*public void createGameTables(GameTemplate gameTemplate, String prefix) {
		for(GameTable gt : gameTemplate.getTables) {
			List<ColData> cols =  new ArrayList<>();
			for(ColData col : gt.getCols) {
				cols.add(col);
			}
			createTable(prefix+"_"+gt.getName(), cols);
		}
	}*/
	
	public boolean checkPlayer(String playerId) throws SQLException {
		return selectAllWhere("players", "player_uniqe_id="+playerId).size() > 0;
	}
	
	public int writeNewApiKey(String ownerEmail, String apiKey) throws SQLException {
		List<CellData> row = new ArrayList<>();
		int ownerId = getOwnerIdByEmail(ownerEmail);
		
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
	
	public int regOwner(String ownerEmail) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData(Types.VARCHAR, "email", ownerEmail));
		return insertIntoTable("owners", row);
	}
	
	public boolean checkOwnerByEmail(String ownerEmail) throws SQLException {
		return selectAllWhere("owners", "email="+ownerEmail).size() > 0;
	}
	
	public int getOwnerIdByEmail(String ownerEmail) throws SQLException {
		List<List<CellData>> owners = selectAllWhere("owners", "email="+ownerEmail);
		
		if(owners.size() > 0) {
			return (int)owners.get(0).get(0).getValue();
		}
		
		return 0;
	}
	
	
	public boolean checkGameByKey(String apiKey) throws SQLException {
		
		if(tablesData.containsKey(apiKey)) {
			return true;
		}
		
		return selectAllWhere("games", "api_key="+apiKey).size() > 0;
	}
	
	public GameEntity getGameByKey(String apiKey) throws SQLException {
		
		List<List<CellData>> games = selectAllWhere("games", "api_key="+apiKey);
		if(games.size() < 1 || games.get(0).size() < 1) {
			return null;
		}
		
		return new GameEntity((int)games.get(0).get(0).getValue(), 
				(String)games.get(0).get(1).getValue(), 
				(String)games.get(0).get(2).getValue(), 
				(int)games.get(0).get(3).getValue(), 
				(String)games.get(0).get(4).getValue(), 
				(String)games.get(0).get(5).getValue());
	}
	
	public boolean deleteGameByApiKey(String apiKey) throws SQLException {
		GameEntity game = getGameByKey(apiKey);
		
		if(game == null) {
			return false;
		}
		String[] tables = game.getTables().split(",");
		for(String table : tables) {
			dropTable(table);
		}
		deleteFrom("games", "api_key='"+apiKey+"'");
		return true;
	}

	public int readOwnerIdFromApiKeys(String apiKey) throws SQLException {
		List<List<CellData>> res = selectAllWhere("api_keys", "api_key="+apiKey);
		
		if(res.size() < 1 || res.get(0).size() < 1) {
			return -1;
		}
		
		return (int) res.get(0).get(1).getValue();
	}
	
	public void removeApiKey(String apiKey) throws SQLException {
		deleteFrom("api_keys", "api_key='"+apiKey+"'");
	}
	
	/*GameEntity selectGameByApiKey(String gameApiKey) throws SQLException {
		PreparedStatement pstmt = getCon().prepareStatement("SELECT * FROM games WHERE games.key LIKE ?");
		pstmt.setString(1, gameApiKey);
		ResultSet resultSet = pstmt.executeQuery();
		
		if(resultSet.next()) {
			int id = resultSet.getInt(1);
			String name = resultSet.getString(2);
			int ownerId = resultSet.getInt(3);
			String key = resultSet.getString(4);
			return new GameEntity(id, name, ownerId, key);
		}
		
		return null;
	}*/
	
	int insertOwner(String name) throws SQLException {
		PreparedStatement pstmt1 = getCon().prepareStatement("SELECT game_owners.name FROM game_owners WHERE game_owners.name=?");
		pstmt1.setString(1, name);
		ResultSet resultSet = pstmt1.executeQuery();
		if(resultSet.first()) {
			return 0;
		}
		PreparedStatement pstmt2 = getCon().prepareStatement("INSERT INTO game_owners (name) VALUES (?)");
		pstmt2.setString(1, name);
		return pstmt2.executeUpdate();
	}
	
	public GameOwnerEntity selectOwnerByName(String name) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = getCon().prepareStatement("SELECT * FROM game_owners WHERE game_owners.name=?");
		pstmt1.setString(1, name);
		resultSet = pstmt1.executeQuery();
		if(resultSet.next()) {
			int ownerId = resultSet.getInt(1);
			String ownerName = resultSet.getString(2);
			return new GameOwnerEntity(ownerId, ownerName);
		}
			
		return null;
	}
	
	private int insertGame(String gameName, String gameJavaPackage, int ownerId, String key, String tables) throws SQLException {
		PreparedStatement pstmt = getCon().prepareStatement("INSERT INTO games (`name`, `package`, `owner_id`, `api_key`, `tables`) VALUES (?, ?, ?, ?, ?)");
		pstmt.setString(1, gameName);
		pstmt.setString(2, gameJavaPackage);
		pstmt.setInt(3, ownerId);
		pstmt.setString(4, key);
		pstmt.setString(5, tables);
		return pstmt.executeUpdate();
	}
	

	
	List<GameEntity> selectGames() throws SQLException {
		List<GameEntity> result = new ArrayList<GameEntity>();
		
		List<List<CellData>> games = selectAll("games");
		for(List<CellData> r : games) {
			result.add(new GameEntity((int)r.get(0).getValue(), 
					(String)r.get(1).getValue(), 
					(String)r.get(2).getValue(),
					(int)   r.get(3).getValue(), 
					(String)r.get(4).getValue(), 
					(String)r.get(5).getValue()));
		}
		return result;
	}

	public int insertGame(String gameName, String gameJavaPackage, int ownerId, String apiKey, String[] tableNameSet, String prefix) throws SQLException {
		
		StringBuilder tables = new StringBuilder();
		
		for (int i = 0; i < tableNameSet.length; i++) {
			tables.append(prefix).append(tableNameSet[i]);
			if(i < tableNameSet.length-1) {
				tables.append(",");
			}
		}
		
		return insertGame(gameName, gameJavaPackage, ownerId, apiKey, tables.toString());
	}
}
