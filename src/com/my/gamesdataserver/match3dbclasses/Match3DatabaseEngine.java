package com.my.gamesdataserver.match3dbclasses;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.rawdbclasses.CellData;
import com.my.gamesdataserver.rawdbclasses.DataBaseInterface;

public class Match3DatabaseEngine {
	private DataBaseInterface dataBaseInterface;
	private String tablePrefix;
	
	public Match3DatabaseEngine(DataBaseInterface dataBaseInterface) {
		this.dataBaseInterface = dataBaseInterface;
	}
	
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	
	public Match3PlayerData readPlayerData(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Match3Level> levels = getLevelsOfPlayer(playerId/*, tablePrefix*/);
		List<Match3Boost> boosts = getBoostsOfPlayer(playerId/*, tablePrefix*/);
		return new Match3PlayerData(levels.size(), levels, boosts);
	}
	
	public List<Match3Level> getLevelsOfPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Match3Level> levels = new ArrayList<Match3Level>();
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"scorelevel", "playerId="+playerId);
		
		for(List<CellData> row : rows) {
			levels.add(new Match3Level((int)row.get(0).getValue(), 
									   (int)row.get(1).getValue(), 
									   (int)row.get(2).getValue(), 
									   (int)row.get(3).getValue(), 
									   (int)row.get(4).getValue()));
		}
		
		return levels;
	}
	
	public List<Match3Boost> getBoostsOfPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		List<Match3Boost> boosts = new ArrayList<Match3Boost>();
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"boosts", "playerId='"+playerId+"'");
		
		for(List<CellData> row : rows) {
			boosts.add(new Match3Boost((int)row.get(0).getValue(), 
									   (int)row.get(1).getValue(), 
									   (String)row.get(2).getValue(), 
									   (int)row.get(3).getValue()));
		}
		
		return boosts;
	}
	
	public Match3Player getPlayer(String playerId/*, String tablePrefix*/) throws SQLException {
		
		List<List<CellData>> rows = dataBaseInterface.selectAllWhere(tablePrefix+"players", "playerId="+playerId);
		
		if(rows.size() < 1) {
			return null;
		}
		
		return new Match3Player((int)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (int)rows.get(0).get(2).getValue());
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
