package com.my.gamesdataserver.dbengineclasses;

import java.sql.Connection;
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

import org.json.JSONException;

import com.my.gamesdataserver.DataBaseConnectionParameters;
import com.my.gamesdataserver.DatabaseConnectionManager;
import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Field;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlExpression;
import com.my.gamesdataserver.basedbclasses.SqlInsert;
import com.my.gamesdataserver.basedbclasses.SqlSelect;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.basedbclasses.SqlExpression.Cond;
import com.my.gamesdataserver.basedbclasses.SqlWhereValue;
import com.my.gamesdataserver.helpers.RandomKeyGenerator;
import com.my.gamesdataserver.template1classes.Player;

public class DataBaseMethods  {
	
	public static void createGameTables(GameTemplate gameTemplate, String prefix, Connection connection) throws SQLException {
		for(TableTemplate tableTemplate : gameTemplate.getTableTemplates()) {
			createGameTable(tableTemplate, prefix, connection);
		}
	}
	
	public static void createGameTable(TableTemplate table, String prefix, Connection connection) throws SQLException {
			
		SqlMethods.createTable(prefix+table.getName(), table.getCols(), table.getPrimaryKey(), connection);
			
		for(TableIndex tIndex : table.getIndices()) {
			SqlMethods.createIndex(tIndex.getName(), prefix+table.getName(), tIndex.getFields(), tIndex.isUnique(), connection);
		}
	}
	
	public static int updateGame(String name, String gameJavaPackage) {
		return 0;
	}
	
	public static int regOwner(String email, Connection connection) throws SQLException {
		Row row = new Row();
		row.addCell(new CellData(Types.VARCHAR, "email", email));
		List<Row> rows = new ArrayList<>();
		rows.add(row);
		return SqlMethods.insert("owners", rows, connection);
	}
	
	public static boolean checkGameByKey(String apiKey, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "api_key", apiKey)));
		return SqlMethods.select("games", where, true, connection).size() > 0;
	}
	
	public static Game deleteGame(String apiKey, Connection connection) throws SQLException {
		Game game = getGameByKey(apiKey, connection);
		
		if(game != null) {
			//SqlMethods.deleteFrom("games", "api_key='"+apiKey+"'", connection);
			return game;
		}
		
		return null;
	}
	
	public static void deleteGameTables(String prefix, String[] templateNames, Connection connection) throws SQLException {
		/*for(String tableName : templateNames) {
			SqlMethods.dropTable(prefix+tableName, connection);
		}*/
	}
	
	public static int insertGame(String gameName, int ownerId, String apiKey, String apiSecret, String prefix, String hash, Connection connection) throws SQLException {
		
		List<Row> rows = new ArrayList<>();
		Row row = new Row();
		row.addCell(new CellData(Types.VARCHAR, "name", gameName));
		row.addCell(new CellData(Types.INTEGER, "owner_id", ownerId));
		row.addCell(new CellData(Types.VARCHAR, "api_key", apiKey));
		row.addCell(new CellData(Types.VARCHAR, "api_secret", apiSecret));
		row.addCell(new CellData(Types.VARCHAR, "prefix", prefix));
		row.addCell(new CellData(Types.VARCHAR, "hash", hash));
		rows.add(row);
		return SqlMethods.insert("games", rows, connection);
	}
	
	public static String generateTablePrefix(String gameName, String apiKey) {
		return (gameName.replaceAll(" ", "_")+"_"+apiKey.substring(0, 8)+"_").toLowerCase();
	}
	
	public static String[] getTablesNamesOfGame(String dataBaseName, String prefix, Connection connection) throws SQLException {
		return SqlMethods.findTablesByPrefix(dataBaseName, prefix, connection);
	}

	/*public boolean isTransactionsEnabled() throws SQLException {
		return dataBaseInterface.isTransactionsEnabled();
	}

	public void rollback() throws SQLException {
		dataBaseInterface.rollback();
	}

	public void enableTransactions() throws SQLException {
		dataBaseInterface.enableTransactions();
	}

	public void commit() throws SQLException {
		dataBaseInterface.commit();
	}

	public void disableTransactions() throws SQLException {
		dataBaseInterface.disableTransactions();
	}*/

	public static List<Row> executeSelect(SqlSelect sqlRequest, Connection connection) throws SQLException {
		return SqlMethods.select(sqlRequest.getTableName(), false, connection);
	}

	public static int executeInsert(SqlInsert sqlRequest, Connection connection) throws SQLException {
		
		return SqlMethods.insert(sqlRequest.getTableName(), sqlRequest.getRows(), connection);
	}

	public static int executeUpdate(SqlUpdate sqlRequest, Connection connection) throws SQLException {
		return SqlMethods.updateTable(sqlRequest.getTableName(), sqlRequest.getSet(), sqlRequest.getWhere(), connection);
	}
	
	public static Game getGameByKey(String apiKey, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "api_key", apiKey)));
		return getGame(where, connection);
	}

	public static Game getGameByHash(String gameHash, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "hash", gameHash)));
		return getGame(where, connection);
	}
	
	private static Game getGame(SqlExpression where, Connection connection) throws SQLException {
		Game game = null;
		List<Row> rows = SqlMethods.select("games", where, true, connection);
		
		if(rows.size() > 0) {
			Row firstRow = rows.get(0);
			int id = (int) firstRow.get("id");
			String gameName = (String)firstRow.get("name");
			String javaPackage = (String)firstRow.get("package");
			int ownerId = (int)firstRow.get("owner_id");
			String apiKey = (String)firstRow.get("api_key");
			String secretKey = (String)firstRow.get("api_secret");
			String type = (String)firstRow.get("type");
			String prefix = (String)firstRow.get("prefix");
			String hash = (String)firstRow.get("hash");
			game = new Game(id, gameName, javaPackage, ownerId, apiKey, secretKey, type, prefix, hash);
		}
		
		return game;
	}

	public static Player getPlayerById(String playerId, String gamePrefix, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "playerId", playerId)));
		return getPlayer(gamePrefix+"players", where, connection);
	}
	
	public static Player getPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "facebookId", facebookId)));
		return getPlayer(gamePrefix+"players", where, connection);
	}
	
	private static Player getPlayer(String tableName, SqlExpression expression, Connection connection) throws SQLException {
		List<Row> rows = SqlMethods.select(tableName, expression, false, connection);
		
		if(rows.size() <= 0) {
			return null;
		}
		
		Row firstRow = rows.get(0);
		
		return new Player((String)firstRow.get("playerId"), (String)firstRow.get("facebookId"));
	}
	
	public static String registrationPlayerByFacebookId(String facebookId, String gamePrefix, Connection connection) throws SQLException {
		List<Row> rows = new ArrayList<>();
		String playerId = generatePlayerId();
		Row row = new Row();
		row.addCell(new CellData(Types.VARCHAR, "playerId", playerId));
		row.addCell(new CellData(Types.VARCHAR, "facebookId", facebookId));
		rows.add(row);
		if(SqlMethods.insert(gamePrefix+"players", rows, connection) <= 0) {
			return null;
		}
		return playerId;
	}
	
	private static String generatePlayerId() {
		return RandomKeyGenerator.nextString(8).toLowerCase()+"-"+RandomKeyGenerator.nextString(8).toLowerCase();
	}
	
	public static List<SpecialRequest> readSpecialRequests(int gameId, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "game_id", gameId)));
		List<Row> resultRows = SqlMethods.select("special_requests", where, false, connection);
		
		List<SpecialRequest> result = new ArrayList<>();
		
		for(Row row : resultRows) {
			int fetchedGameId = (int) row.get("game_id");
			String requestName = (String) row.get("request_name");
			String table = (String) row.get("query_table");
			String fields = (String) row.get("fields");
			result.add(new SpecialRequest(fetchedGameId, requestName, table, fields));
		}
			
		return result;
	}
	
	public static SpecialRequest readSpecialRequest(int gameId, String requestName, Connection connection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "game_id", gameId)));
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "request_name", requestName)).withCond(Cond.AND));
		List<Row> resultRows = SqlMethods.select("special_requests", where, true, connection);
		
		SpecialRequest result = null;
		
		if(resultRows.size() > 0) {
			int fetchedGameId = (int) resultRows.get(0).get("game_id");
			String fetchedRequestName = (String) resultRows.get(0).get("request_name");
			String table = (String) resultRows.get(0).get("query_table");
			String fields = (String) resultRows.get(0).get("fields");
			result = new SpecialRequest(fetchedGameId, fetchedRequestName, table, fields);
		}
			
		return result;
	}
	
	/*public static int setSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		
		String whereLiteral = "game_id="+gameId+"&request_name="+requestName;
		String setClause = "game_id="+gameId+"&request_name="+requestName+"&query_table="+table+"&fields="+fields;
		
		if(SqlMethods.selectAll("special_requests", whereLiteral, connection).size() > 0) {
			return SqlMethods.updateTable("special_requests", setClause, whereLiteral, connection);
		}
		return SqlMethods.insert("special_requests", setClause, connection);
	}*/
	
	public static int addSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		List<Row> rows = new ArrayList<>();
		Row row = new Row();
		row.addCell(new CellData(Types.VARCHAR, "request_name",  requestName));
		row.addCell(new CellData(Types.VARCHAR, "query_table",  table));
		row.addCell(new CellData(Types.VARCHAR, "fields",  fields));
		rows.add(row);
		return SqlMethods.insert("special_requests", rows, connection);
	}
	
	/*public static void updateSpecialRequest(int gameId, String requestName, String table, String fields, Connection connection) throws SQLException {
		String whereLiteral = "game_id="+gameId+"&request_name="+requestName;
		String setClause = "request_name="+requestName+"&query_table="+table+"&fields="+fields;
		SqlMethods.updateTable("special_requests", setClause, whereLiteral, connection);
	}*/
	
	public static List<Row> executeSpecialRequest(SpecialRequest specialRequest, String tablePrefix, Connection connection) throws JSONException, SQLException {
		SqlSelect s = new SqlSelect(tablePrefix+specialRequest.getTable()).withFields(specialRequest.getFieldsList());
		return SqlMethods.select(s.getTableName(), false, connection);
	}

	public static Owner getOwnerByEmail(String email, Connection dbConnection) throws SQLException {
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "email", email)));
		List<Row> result = SqlMethods.select("owners", where, true, dbConnection);
		if(result.size() <= 0) {
			return new Owner((int) result.get(0).get("id"), (String) result.get(0).get("email"));
		}
		return null;
	}
	
	public static int serverTypeToMysqType(String serverType) {
		switch (serverType) {
		case "integer":
			return Types.INTEGER;
		case "string":
			return Types.VARCHAR;
		case "float":
			return Types.FLOAT;
		}
		return Types.NULL;
	}
}
