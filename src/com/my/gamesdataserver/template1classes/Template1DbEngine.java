package com.my.gamesdataserver.template1classes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.SqlExpression;
import com.my.gamesdataserver.basedbclasses.SqlMethods;

public class Template1DbEngine {
	private String tablePrefix;
	
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	
	public PlayerData readPlayerData(String playerId) throws SQLException {
		List<Level> levels = getLevelsOfPlayer(playerId);
		List<Boost> boosts = getBoostsOfPlayer(playerId);
		return new PlayerData(levels.size(), levels, boosts);
	}
	
	public List<Level> getLevelsOfPlayer(String playerId) throws SQLException {
		/*List<Level> levels = new ArrayList<Level>();
		List<List<CellData>> rows = dataBaseInterface.selectAll(tablePrefix+"levels", "playerId="+playerId);
		
		for(List<CellData> row : rows) {
			levels.add(new Level((int)row.get(0).getValue(), 
									   (int)row.get(1).getValue(), 
									   (int)row.get(2).getValue(), 
									   (int)row.get(3).getValue(), 
									   (int)row.get(4).getValue()));
		}
		
		return levels;*/
		return null;
	}
	
	public List<Boost> getBoostsOfPlayer(String playerId) throws SQLException {
		/*List<Boost> boosts = new ArrayList<Boost>();
		List<List<CellData>> rows = dataBaseInterface.selectAll(tablePrefix+"boosts", "playerId='"+playerId+"'");
		
		for(List<CellData> row : rows) {
			boosts.add(new Boost((int)row.get(0).getValue(), (String)row.get(1).getValue(), (int)row.get(2).getValue()));
		}
		
		return boosts;*/
		return null;
	}
	
	public Player getPlayer(String playerId) throws SQLException {
		
		/*List<List<CellData>> rows = dataBaseInterface.selectAll(tablePrefix+"players", "playerId="+playerId);
		
		if(rows.size() < 1) {
			return null;
		}
		
		return new Player((String)rows.get(0).get(0).getValue(), (String)rows.get(0).get(1).getValue(), (int)rows.get(0).get(2).getValue());*/
		return null;
	}

	/*public void addPlayer(String playerId, Connection connection) throws SQLException {
		List<SqlExpression> row = new ArrayList<>();
		row.add(new SqlExpression("playerId", playerId));
		row.add(new SqlExpression("max_level", 0));
		SqlMethods.insert(tablePrefix+"players", row, connection);
	}

	public int updateLevel(String playerId, int level, int scores, int stars, Connection connection) throws SQLException {
		List<SqlExpression> set = new ArrayList<>();
		
		set.add(new SqlExpression("playerId", playerId));
		set.add(new SqlExpression("level", level));
		set.add(new SqlExpression("score", scores));
		set.add(new SqlExpression("stars", stars));
		
		List<SqlExpression> where = new ArrayList<>();
		
		where.add(new SqlExpression("playerId", playerId));
		where.add(new SqlExpression("level", level));
		
		return SqlMethods.updateTable(tablePrefix+"scorelevel", set, where, connection);
	}

	public int addLevel(String playerId, int level, int scores, int stars, Connection connection) throws SQLException {
		List<SqlExpression> row = new ArrayList<>();
		
		row.add(new SqlExpression("playerId", playerId));
		row.add(new SqlExpression("level", level));
		row.add(new SqlExpression("score", scores));
		row.add(new SqlExpression("stars", stars));
		
		return SqlMethods.insert(tablePrefix+"scorelevel", row, connection);
	}*/
	
	/*public String getPlayersTableName(String apiKey) throws SQLException {
		Game game = getGameByKey(apiKey);
		
		if(game == null) {
			return null;
		}
		
		return game.getPrefix()+"players";
	}*/

}
