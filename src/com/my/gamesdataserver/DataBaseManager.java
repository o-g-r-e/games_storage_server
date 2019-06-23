package com.my.gamesdataserver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataBaseManager {
	private Connection con;

	public DataBaseManager(String url, String user, String password) throws SQLException {
		con = DriverManager.getConnection(url, user, password);
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
	
	GameSaveData selectSaves(String key, String playerId) throws SQLException {
		GameSaveData result = new GameSaveData();
		ResultSet resultSet = null;
		PreparedStatement pstmt = null;
		pstmt = con.prepareStatement("SELECT epsilon.saves.level_stars, epsilon.boosts.boost_data\r\n" + 
				"FROM epsilon.saves\r\n" + 
				"LEFT JOIN (SELECT * FROM epsilon.games WHERE epsilon.games.key LIKE ?) as gms ON epsilon.saves.game_id = gms.id\r\n" + 
				"LEFT JOIN epsilon.boosts ON epsilon.boosts.game_id = epsilon.saves.game_id AND epsilon.boosts.player_id = epsilon.saves.player_id\r\n" + 
				"WHERE \r\n" + 
				"epsilon.saves.player_id IN (SELECT epsilon.players.id FROM epsilon.players WHERE epsilon.players.player_id LIKE ?)\r\n" + 
				"ORDER BY epsilon.saves.level_num");
		pstmt.setString(1, key);
		pstmt.setString(2, playerId);
		resultSet = pstmt.executeQuery();
		int i = 0;
		while(resultSet.next()) {
			result.addStars(resultSet.getInt(1));
			if(i==0) {
				result.setBoostData(resultSet.getString(2).replace("\"", "\\\""));
			}
			i++;
		}
		if(i==0) {
			return null;
		}
		return result;
	}
	
	int updateLevel(String key, String playerId, int level, int stars) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT epsilon.games.id, epsilon.players.id FROM epsilon.games, epsilon.players WHERE epsilon.games.key LIKE ? AND epsilon.players.player_id LIKE ?");
		pstmt1.setString(1, key);
		pstmt1.setString(2, playerId);
		resultSet = pstmt1.executeQuery();
		int gameId = 0;
		int playerIntId = 0;
		if(resultSet.next()) {
			gameId = resultSet.getInt(1);
			playerIntId = resultSet.getInt(2);
		} else {
			return 0;
		}
		
		PreparedStatement pstmt2 = con.prepareStatement("UPDATE epsilon.saves SET level_stars=? WHERE game_id=? AND player_id=? AND level_num=?");
		pstmt2.setInt(1, stars);
		pstmt2.setInt(2, gameId);
		pstmt2.setInt(3, playerIntId);
		pstmt2.setInt(4, level);
		return pstmt2.executeUpdate();
	}
	
	int updateBoostData(String key, String playerId, String boostData) throws SQLException {
		ResultSet resultSet = null;
		
		PreparedStatement pstmt1 = con.prepareStatement("SELECT epsilon.games.id, epsilon.players.id FROM epsilon.games, epsilon.players WHERE epsilon.games.key LIKE ? AND epsilon.players.player_id LIKE ?");
		pstmt1.setString(1, key);
		pstmt1.setString(2, playerId);
		resultSet = pstmt1.executeQuery();
		int gameId = 0;
		int playerIntId = 0;
		if(resultSet.next()) {
			gameId = resultSet.getInt(1);
			playerIntId = resultSet.getInt(2);
		} else {
			return 0;
		}
		
		PreparedStatement pstmt2 = con.prepareStatement("SELECT * FROM epsilon.boosts WHERE epsilon.boosts.game_id=? AND epsilon.boosts.player_id=?");
		pstmt2.setInt(1, gameId);
		pstmt2.setInt(2, playerIntId);
		resultSet = pstmt2.executeQuery();
		String request = "INSERT INTO epsilon.boosts (boost_data, game_id, player_id) VALUES (?, ?, ?)";
		if(resultSet.next()) {
			request = "UPDATE epsilon.boosts SET boost_data=? WHERE game_id=? AND player_id=?";
		} 
		PreparedStatement pstmt3 = con.prepareStatement(request);
		pstmt3.setString(1, boostData);
		pstmt3.setInt(2, gameId);
		pstmt3.setInt(3, playerIntId);
		return pstmt3.executeUpdate();
	}
	
	int insertLevel(String key, String playerId, int level, int stars) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT epsilon.games.id, epsilon.players.id FROM epsilon.games, epsilon.players WHERE epsilon.games.key LIKE ? AND epsilon.players.player_id LIKE ?");
		pstmt1.setString(1, key);
		pstmt1.setString(2, playerId);
		resultSet = pstmt1.executeQuery();
		int gameId = 0;
		int playerIntId = 0;
		if(resultSet.next()) {
			gameId = resultSet.getInt(1);
			playerIntId = resultSet.getInt(2);
		} else {
			return 0;
		}
		
		PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO epsilon.saves (game_id, player_id, level_num, level_stars) VALUES (?, ?, ?, ?)");
		pstmt2.setInt(1, gameId);
		pstmt2.setInt(2, playerIntId);
		pstmt2.setInt(3, level);
		pstmt2.setInt(4, stars);
		return pstmt2.executeUpdate();
	}
	
	int insertPlayer(String name, String playerid) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO epsilon.players (name, player_id) VALUES (?, ?)");
		pstmt.setString(1, name);
		pstmt.setString(2, playerid);
		return pstmt.executeUpdate();
	}
	
	int insertOwner(String name) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO epsilon.game_owners (name) VALUES (?)");
		pstmt.setString(1, name);
		return pstmt.executeUpdate();
	}
	
	int insertGame(String ownerName, String gameName, String key) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT epsilon.game_owners.id FROM epsilon.game_owners WHERE epsilon.game_owners.name=?");
		pstmt1.setString(1, ownerName);
		resultSet = pstmt1.executeQuery();
		int ownerId = 0;
		if(resultSet.next()) {
			ownerId = resultSet.getInt(1);
		} else {
			return 0;
		}
		PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO epsilon.games (`name`, owner_id, `key`) VALUES (?, ?, ?)");
		pstmt2.setString(1, gameName);
		pstmt2.setInt(2, ownerId);
		pstmt2.setString(3, key);
		return pstmt2.executeUpdate();
	}
	
	List<BoostEntity> selectFromBoosts() throws SQLException {
		List<BoostEntity> result = new ArrayList<BoostEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM epsilon.boosts");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			List<BoostEntity> row = new ArrayList<BoostEntity>();
			result.add(new BoostEntity(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getString(4)));
		}
		return result;
	}
	
	List<GameOwnerEntity> selectFromGameOwners() throws SQLException {
		List<GameOwnerEntity> result = new ArrayList<GameOwnerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM epsilon.game_owners");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new GameOwnerEntity(resultSet.getInt(1), resultSet.getString(2)));
		}
		return result;
	}
	
	List<GameEntity> selectFromGames() throws SQLException {
		List<GameEntity> result = new ArrayList<GameEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM epsilon.games");
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
	
	List<PlayerEntity> selectFromPlayers() throws SQLException {
		List<PlayerEntity> result = new ArrayList<PlayerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM epsilon.players");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new PlayerEntity(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3)));
		}
		return result;
	}
	
	List<SaveEntity> selectFromSaves() throws SQLException {
		List<SaveEntity> result = new ArrayList<SaveEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM epsilon.saves");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new SaveEntity(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getInt(4), resultSet.getInt(5)));
		}
		return result;
	}
}
