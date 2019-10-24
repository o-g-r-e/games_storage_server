package com.my.gamesdataserver.defaultgameclasses;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.DataBaseInterface;

public class BaseGameDbInterface {
	private DataBaseInterface dataBaseInterface;
	private String tablePrefix;
	
	public BaseGameDbInterface(DataBaseInterface dataBaseInterface) {
		this.dataBaseInterface = dataBaseInterface;
	}
	
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	
	public PlayerData readPlayerData(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Level> levels = getLevelsOfPlayer(playerId/*, tablePrefix*/);
		List<Boost> boosts = getBoostsOfPlayer(playerId/*, tablePrefix*/);
		return new PlayerData(levels.size(), levels, boosts);
	}
	
	public List<Level> getLevelsOfPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Level> levels = new ArrayList<Level>();
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"scorelevel", "playerId="+playerId);
		
		for(List<CellData> row : rows) {
			levels.add(new Level((int)row.get(0).getValue(), 
									   (int)row.get(1).getValue(), 
									   (int)row.get(2).getValue(), 
									   (int)row.get(3).getValue(), 
									   (int)row.get(4).getValue()));
		}
		
		return levels;
	}
	
	public List<Boost> getBoostsOfPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Boost> boosts = new ArrayList<Boost>();
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"boosts", "playerId='"+playerId+"'");
		
		for(List<CellData> row : rows) {
			boosts.add(new Boost((int)row.get(0).getValue(), 
									   (int)row.get(1).getValue(), 
									   (String)row.get(2).getValue(), 
									   (int)row.get(3).getValue()));
		}
		
		return boosts;
	}
	
	public Player getPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"players", "playerId="+playerId);
		
		if(rows.size() < 1) {
			return null;
		}
		
		return new Player((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (int)rows.get(0).get(2).getValue());
	}

	public void addPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		List<CellData> row = new ArrayList<>();
		row.add(new CellData("playerId", playerId));
		row.add(new CellData("max_level", 0));
		dataBaseInterface.insertIntoTable(tablePrefix+"players", row);
	}

	public int updateLevel(String playerId, int level, int scores, int stars) throws SQLException {
		List<CellData> set = new ArrayList<>();
		
		set.add(new CellData("playerId", playerId));
		set.add(new CellData("level", level));
		set.add(new CellData("score", scores));
		set.add(new CellData("stars", stars));
		
		List<CellData> where = new ArrayList<>();
		
		where.add(new CellData("playerId", playerId));
		where.add(new CellData("level", level));
		
		return dataBaseInterface.updateTable(tablePrefix+"scorelevel", set, where);
	}

	public int addLevel(String playerId, int level, int scores, int stars) throws SQLException {
		List<CellData> row = new ArrayList<>();
		
		row.add(new CellData("playerId", playerId));
		row.add(new CellData("level", level));
		row.add(new CellData("score", scores));
		row.add(new CellData("stars", stars));
		
		return dataBaseInterface.insertIntoTable(tablePrefix+"scorelevel", row);
	}
	
	/*public String getPlayersTableName(String apiKey) throws SQLException {
		Game game = getGameByKey(apiKey);
		
		if(game == null) {
			return null;
		}
		
		return game.getPrefix()+"players";
	}*/

}