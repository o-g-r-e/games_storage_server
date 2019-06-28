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
	
	GameSaveData selectSaves(String key, String playerId) throws SQLException {
		GameSaveData result = new GameSaveData();
		ResultSet resultSet = null;
		PreparedStatement pstmt = null;
		pstmt = con.prepareStatement("SELECT saves.level_stars, boosts.boost_data\r\n" + 
				"FROM saves\r\n" + 
				"LEFT JOIN (SELECT * FROM games WHERE games.key LIKE ?) as gms ON saves.game_id = gms.id\r\n" + 
				"LEFT JOIN boosts ON boosts.game_id = saves.game_id AND boosts.player_id = saves.player_id\r\n" + 
				"WHERE \r\n" + 
				"saves.player_id IN (SELECT players.id FROM players WHERE players.id LIKE ?)\r\n" + 
				"ORDER BY saves.level_num");
		pstmt.setString(1, key);
		pstmt.setString(2, playerId);
		if(printQuery) {
			System.out.println(pstmt);
		}
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
		PreparedStatement pstmt1 = con.prepareStatement("SELECT games.id, players.id FROM games, players WHERE games.key LIKE ? AND players.id LIKE ?");
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
		
		PreparedStatement pstmt2 = con.prepareStatement("UPDATE saves SET level_stars=? WHERE game_id=? AND player_id=? AND level_num=?");
		pstmt2.setInt(1, stars);
		pstmt2.setInt(2, gameId);
		pstmt2.setInt(3, playerIntId);
		pstmt2.setInt(4, level);
		return pstmt2.executeUpdate();
	}
	
	int updateBoostData(String key, String playerId, String boostData) throws SQLException {
		ResultSet resultSet = null;
		
		PreparedStatement pstmt1 = con.prepareStatement("SELECT games.id, players.id FROM games, players WHERE games.key LIKE ? AND players.id LIKE ?");
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
		
		PreparedStatement pstmt2 = con.prepareStatement("SELECT * FROM boosts WHERE boosts.game_id=? AND boosts.player_id=?");
		pstmt2.setInt(1, gameId);
		pstmt2.setInt(2, playerIntId);
		resultSet = pstmt2.executeQuery();
		String request = "INSERT INTO boosts (boost_data, game_id, player_id) VALUES (?, ?, ?)";
		if(resultSet.next()) {
			request = "UPDATE boosts SET boost_data=? WHERE game_id=? AND player_id=?";
		} 
		PreparedStatement pstmt3 = con.prepareStatement(request);
		pstmt3.setString(1, boostData);
		pstmt3.setInt(2, gameId);
		pstmt3.setInt(3, playerIntId);
		return pstmt3.executeUpdate();
	}
	
	int insertLevel(String key, String playerId, int level, int stars) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT games.id, players.id FROM games, players WHERE games.key LIKE ? AND players.id LIKE ?");
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
		
		PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO saves (game_id, player_id, level_num, level_stars) VALUES (?, ?, ?, ?)");
		pstmt2.setInt(1, gameId);
		pstmt2.setInt(2, playerIntId);
		pstmt2.setInt(3, level);
		pstmt2.setInt(4, stars);
		return pstmt2.executeUpdate();
	}
	
	int insertPlayer(String name, String playerid) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO players (name, player_id) VALUES (?, ?)");
		pstmt.setString(1, name);
		pstmt.setString(2, playerid);
		return pstmt.executeUpdate();
	}
	
	int insertOwner(String name) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO game_owners (name) VALUES (?)");
		pstmt.setString(1, name);
		return pstmt.executeUpdate();
	}
	
	int insertGame(String ownerName, String gameName, String key) throws SQLException {
		ResultSet resultSet = null;
		PreparedStatement pstmt1 = con.prepareStatement("SELECT game_owners.id FROM game_owners WHERE game_owners.name=?");
		pstmt1.setString(1, ownerName);
		resultSet = pstmt1.executeQuery();
		int ownerId = 0;
		if(resultSet.next()) {
			ownerId = resultSet.getInt(1);
		} else {
			return 0;
		}
		PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO games (`name`, owner_id, `key`) VALUES (?, ?, ?)");
		pstmt2.setString(1, gameName);
		pstmt2.setInt(2, ownerId);
		pstmt2.setString(3, key);
		return pstmt2.executeUpdate();
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
	
	List<GameOwnerEntity> selectFromGameOwners() throws SQLException {
		List<GameOwnerEntity> result = new ArrayList<GameOwnerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM game_owners");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new GameOwnerEntity(resultSet.getInt(1), resultSet.getString(2)));
		}
		return result;
	}
	
	List<GameEntity> selectFromGames() throws SQLException {
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
	
	List<PlayerEntity> selectFromPlayers() throws SQLException {
		List<PlayerEntity> result = new ArrayList<PlayerEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM players");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new PlayerEntity(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3)));
		}
		return result;
	}
	
	List<SaveEntity> selectFromSaves() throws SQLException {
		List<SaveEntity> result = new ArrayList<SaveEntity>();
		
		PreparedStatement pstmt = con.prepareStatement("SELECT * FROM saves");
		ResultSet resultSet = pstmt.executeQuery();
		while(resultSet.next()) {
			result.add(new SaveEntity(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3), resultSet.getInt(4), resultSet.getInt(5)));
		}
		return result;
	}
}
