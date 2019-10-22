package com.my.gamesdataserver.gamesdbclasses;

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
import com.my.gamesdataserver.rawdbclasses.CellData;
import com.my.gamesdataserver.rawdbclasses.ColData;
import com.my.gamesdataserver.rawdbclasses.DataBaseInterface;

public class DatabaseEngine  {
	
	private DataBaseInterface dataBaseInterface;

	public DatabaseEngine(DataBaseInterface dataBaseInterface) {
		this.dataBaseInterface = dataBaseInterface;
	}
	
	public void createGameTables(GameTemplate gameTemplate, String prefix) throws SQLException {
		for(TableTemplate tt : gameTemplate.getTables()) {
			dataBaseInterface.createTable(prefix+tt.getName(), tt.getCols());
		}
	}
	
	public int writeNewApiKey(String ownerEmail, String apiKey) throws SQLException {
		List<CellData> row = new ArrayList<>();
		int ownerId = getOwnerByEmail(ownerEmail).getId();
		
		if(ownerId > 0) {
			row.add(new CellData(Types.VARCHAR, "owner_id", ownerId));
			row.add(new CellData(Types.VARCHAR, "api_key", apiKey));
			return dataBaseInterface.insertIntoTable("api_keys", row);
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
		return dataBaseInterface.insertIntoTable("owners", row);
	}
	
	public boolean checkOwnerByEmail(String ownerEmail) throws SQLException {
		return dataBaseInterface.selectAllWhere("owners", "email="+ownerEmail).size() > 0;
	}
	
	public Owner getOwnerByEmail(String email) throws SQLException {
		Owner owner = null;
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("owners", "email="+email);
		
		if(rows.size() > 0) {
			owner = new Owner((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue());
		}
		
		return owner;
	}
	public boolean checkGameByKey(String apiKey) throws SQLException {
		
		return dataBaseInterface.selectAllWhere("games", "api_key="+apiKey).size() > 0;
	}
	
	public Game getGameByKey(String apiKey) throws SQLException {
		Game game = null;
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("games", "api_key="+apiKey);
		
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

	public ApiKey getApiKey(String apiKey) throws SQLException {
		ApiKey result = null;
		
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere("api_keys", "api_key="+apiKey);
		
		if(rows.size() > 0) {
			result = new ApiKey((int)rows.get(0).get(0).getValue(), (int)rows.get(0).get(1).getValue(), (String)rows.get(0).get(2).getValue());
		}
		
		return result;
	}
	
	public void removeApiKey(String apiKey) throws SQLException {
		dataBaseInterface.deleteFrom("api_keys", "api_key='"+apiKey+"'");
	}
	
	List<Game> selectGames() throws SQLException {
		List<Game> result = new ArrayList<Game>();
		
		List<List<CellData>> rows = dataBaseInterface.selectAll("games");
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
		
		List<CellData> row = new ArrayList<>();
		
		row.add(new CellData("name", gameName));
		row.add(new CellData("package", gameJavaPackage));
		row.add(new CellData("owner_id", ownerId));
		row.add(new CellData("api_key", key));
		row.add(new CellData("type", type));
		row.add(new CellData("prefix", prefix));
		
		return dataBaseInterface.insertIntoTable("games", row);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
}
