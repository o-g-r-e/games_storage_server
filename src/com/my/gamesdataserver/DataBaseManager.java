package com.my.gamesdataserver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.my.gamesdataserver.dbmodels.BoostEntity;
import com.my.gamesdataserver.dbmodels.GameEntity;
import com.my.gamesdataserver.dbmodels.GameOwnerEntity;
import com.my.gamesdataserver.dbmodels.SaveEntity;
import com.my.gamesdataserver.dbmodels.PlayerEntity;

public class DataBaseManager {
	private Connection con;
	private boolean printQuery = true;

	public DataBaseManager(DataBaseConnectionParameters dbConnectionParameters) throws SQLException {
		con = DriverManager.getConnection(dbConnectionParameters.getUrl(), dbConnectionParameters.getUser(), dbConnectionParameters.getPassword());
	}

	public Connection getCon() {
		return con;
	}
	
	public void closeConnection() {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createTable(String name, MyField[] fields) {
		String query = "CREATE TABLE "+name+" (id INT NOT NULL,";
		
		for (int i = 0; i < fields.length; i++) {
			query += fields[i].name;
			if(fields[i].type == MyField.Type.INTEGER) {
				query += " INT NULL,";
			} else if (fields[i].type == MyField.Type.STRING) {
				query += " VARCHAR(45) NULL,";
			} else if (fields[i].type == MyField.Type.FLOAT) {
				query += " FLOAT NULL,";
			} else {
				return;
			}
		}
		
		query += "PRIMARY KEY (id));";
	}
	
	public int insertToTable(String tableName, List<List<DataCell>> values) throws SQLException {
		StringBuilder fieldsNames = new StringBuilder("(");
		StringBuilder fieldsValues = new StringBuilder("(");
		int width = values.get(0).size();
		for (int i = 0; i < width; i++) {
			DataCell d = values.get(0).get(i);
			fieldsNames.append(d.getName());
			fieldsValues.append("?");
			if(i < width-1) {
				fieldsNames.append(",");
				fieldsValues.append(",");
			}
		}
		
		fieldsNames.append(")");
		fieldsValues.append(")");
		
		StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
		sqlInsert.append(tableName);
		sqlInsert.append(" ");
		sqlInsert.append(fieldsNames);
		sqlInsert.append(" VALUES ");
		sqlInsert.append(fieldsValues);
		for (int i = 1; i < values.size(); i++) {
			sqlInsert.append(",");
			sqlInsert.append(fieldsValues);
		}
		
		PreparedStatement pstmt = con.prepareStatement(sqlInsert.toString());
		
		for (int i = 0; i < values.size(); i++) {
			List<DataCell> row = values.get(i);
			for (int j = 0; j < row.size(); j++) {
				setQueryValue(pstmt, row.get(j), i*row.size()+j+1);
			}
		}
		
		return pstmt.executeUpdate();
	}
	
	public int updateTable(String tableName, List<DataCell> set, List<DataCell> where) throws SQLException {
		StringBuilder sqlUpdate = new StringBuilder("UPDATE ");
		sqlUpdate.append(tableName);
		sqlUpdate.append(" SET ");
		for (int i = 0; i < set.size(); i++) {
			DataCell d = set.get(i);
			sqlUpdate.append(d.getName());
			sqlUpdate.append("=?");
			if(i < set.size()-1) {
				sqlUpdate.append(",");
			}
		}
		sqlUpdate.append(" WHERE (");
		for (int i = 0; i < where.size(); i++) {
			DataCell d = where.get(i);
			sqlUpdate.append(d.getName());
			sqlUpdate.append("=?");
			if(i < set.size()-1) {
				sqlUpdate.append(" AND ");
			}
		}
		sqlUpdate.append(")");
		
		PreparedStatement pstmt = con.prepareStatement(sqlUpdate.toString());
		
		for (int i = 0; i < set.size(); i++) {
			setQueryValue(pstmt, set.get(i), i+1);
		}
		
		for (int i = 0; i < where.size(); i++) {
			setQueryValue(pstmt, where.get(i), i+1+set.size());
		}
		
		return pstmt.executeUpdate();
	}
	
	private void setQueryValue(PreparedStatement pstmt, DataCell d, int index) throws SQLException {
		switch (d.getType()) {
		case Types.INTEGER:
			pstmt.setInt(index, (int)d.getValue());
			break;
		case Types.VARCHAR:
			pstmt.setString(index, String.valueOf(d.getValue()));
			break;
		case Types.FLOAT:
			pstmt.setFloat(index, (float)d.getValue());
			break;
		}
	}
	
	GameEntity selectGameByApiKey(String gameApiKey) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM games WHERE games.key LIKE ?");
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
	}
	
	PlayerEntity selectPlayer(String playerSecretId, int gameId) throws SQLException {
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM players WHERE players.player_uniqe_id LIKE ? AND players.game_id = ?");
		
		pstmt.setString(1, playerSecretId);
		pstmt.setInt(2, gameId);
		ResultSet resultSet = pstmt.executeQuery();
		System.out.println("CLASS_NAME: "+resultSet.getClass().getName());
		if(resultSet.next()) {
			int id = resultSet.getInt(1);
			String name = resultSet.getString(2);
			String selectedPlayerId = resultSet.getString(3);
			int selectedGameId = resultSet.getInt(4);
			return new PlayerEntity(id, name, selectedPlayerId, selectedGameId);
		}
		
		return null;
	}
	
	int insertPlayer(String name, String playerid, int gameId) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO players (name, player_uniqe_id, game_id) VALUES (?, ?, ?)");
		pstmt.setString(1, name);
		pstmt.setString(2, playerid);
		pstmt.setInt(3, gameId);
		return pstmt.executeUpdate();
	}
	
	int insertOwner(String name) throws SQLException {
		PreparedStatement pstmt1 = con.prepareStatement("SELECT game_owners.name FROM game_owners WHERE game_owners.name=?");
		pstmt1.setString(1, name);
		ResultSet resultSet = pstmt1.executeQuery();
		if(resultSet.first()) {
			return 0;
		}
		PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO game_owners (name) VALUES (?)");
		pstmt2.setString(1, name);
		return pstmt2.executeUpdate();
	}
	
	public GameOwnerEntity selectOwnerByName(String name) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT * FROM game_owners WHERE game_owners.name=?");
		pstmt1.setString(1, name);
		resultSet = pstmt1.executeQuery();
		if(resultSet.next()) {
			int ownerId = resultSet.getInt(1);
			String ownerName = resultSet.getString(2);
			return new GameOwnerEntity(ownerId, ownerName);
		}
			
		return null;
	}
	
	int insertGame(int ownerId, String gameName, String key) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO games (`name`, owner_id, `key`) VALUES (?, ?, ?)");
		pstmt.setString(1, gameName);
		pstmt.setInt(2, ownerId);
		pstmt.setString(3, key);
		return pstmt.executeUpdate();
	}
	
	SaveEntity selectSave(int gameId, int playerId) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM saves WHERE saves.game_id=? AND saves.player_id=?");
		pstmt.setInt(1, gameId);
		pstmt.setInt(2, playerId);

		ResultSet resultSet = pstmt.executeQuery();
		
		if(resultSet.next()) {
			int id = resultSet.getInt(1);
			int selectedGameId = resultSet.getInt(2);
			int selectedPlayerId = resultSet.getInt(3);
			String saveData = resultSet.getString(4);
			String boostData = resultSet.getString(5);
			return new SaveEntity(id, selectedGameId, selectedPlayerId, saveData, boostData);
		}
		
		return null;
	}
	
	int updateSave(int id, String saveData) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("UPDATE saves SET save_data = ? WHERE (id = ?)");
		pstmt.setString(1, saveData);
		pstmt.setInt(2, id);
		return pstmt.executeUpdate();
	}
	
	int updateBoost(int id, String boostData) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("UPDATE saves SET boost_data = ? WHERE (id = ?)");
		pstmt.setString(1, boostData);
		pstmt.setInt(2, id);
		return pstmt.executeUpdate();
	}
	
	int insertSave(int gameId, int playerId, String saveData, String boostData) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO saves (game_id, player_id, save_data, boost_data) VALUES (?, ?, ?, ?)");
		pstmt.setInt(1, gameId);
		pstmt.setInt(2, playerId);
		pstmt.setString(3, saveData);
		pstmt.setString(4, boostData);
		return pstmt.executeUpdate();
	}
	
	List<BoostEntity> selectFromBoosts() throws SQLException {
		List<BoostEntity> result = new ArrayList<BoostEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM boosts");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			List<BoostEntity> row = new ArrayList<BoostEntity>();
			result.add(new BoostEntity(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getString(4)));
		}
		return result;
	}
	
	List<GameOwnerEntity> selectOwners() throws SQLException {
		List<GameOwnerEntity> result = new ArrayList<GameOwnerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM game_owners");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new GameOwnerEntity(resultSet.getInt(1), resultSet.getString(2)));
		}
		return result;
	}
	
	List<GameEntity> selectGames() throws SQLException {
		List<GameEntity> result = new ArrayList<GameEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM games");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			String key = resultSet.getString(4);
			if(key != null && key.length() > 5) {
				key = key.substring(0, 5) + "...";
			}
			result.add(new GameEntity(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), resultSet.getString(4)));
		}
		return result;
	}
	
	List<PlayerEntity> selectPlayers() throws SQLException {
		List<PlayerEntity> result = new ArrayList<PlayerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM players");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new PlayerEntity(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getInt(4)));
		}
		return result;
	}
	
	List<SaveEntity> selectSaves() throws SQLException {
		List<SaveEntity> result = new ArrayList<SaveEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM saves");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new SaveEntity(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5)));
		}
		return result;
	}
}
