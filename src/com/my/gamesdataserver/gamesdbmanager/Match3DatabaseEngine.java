package com.my.gamesdataserver.gamesdbmanager;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.rawdbmanager.CellData;

public class Match3DatabaseEngine extends DatabaseEngine {

	public Match3DatabaseEngine(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		super(dbConnectionParameters);
	}
	
	public Match3PlayerData readPlayerData(String playerId, String apiKey) {
		Match3PlayerData result = null;
		
		return result;
	}
	
	public Match3Player getPlayer(String playerId, String tablePrefix) throws SQLException {
		
		List<List<CellData>> rows = selectAllWhere(tablePrefix+"players", "playerId="+playerId);
		
		if(rows.size() < 1) {
			return null;
		}
		
		return new Match3Player((String)rows.get(0).get(0).getValue());
	}

	public void addPlayer(String playerId, String tablePrefix) throws SQLException {
		CellData cd = new CellData("playerId", playerId);
		List<CellData> row = new ArrayList<>();
		row.add(cd);
		insertIntoTable(tablePrefix+"players", row);
	}
	
	/*public String getPlayersTableName(String apiKey) throws SQLException {
		Game game = getGameByKey(apiKey);
		
		if(game == null) {
			return null;
		}
		
		return game.getPrefix()+"players";
	}*/

}
