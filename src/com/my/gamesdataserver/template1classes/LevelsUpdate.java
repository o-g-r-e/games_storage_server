package com.my.gamesdataserver.template1classes;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.basedbclasses.CellData;
import com.my.gamesdataserver.basedbclasses.Row;
import com.my.gamesdataserver.basedbclasses.SqlExpression;
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.SqlUpdate;
import com.my.gamesdataserver.basedbclasses.SqlWhereValue;

public class LevelsUpdate {
	class Level {
		private int level;
		private int stars;
		private int score;
		
		public Level(int level, int stars, int score) {
			this.level = level;
			this.stars = stars;
			this.score = score;
		}

		public int getLevel() {
			return level;
		}

		public int getStars() {
			return stars;
		}

		public int getScore() {
			return score;
		}
	}
	
	class LevelsSet {
		private Map<String, Level> levels;

		public LevelsSet(JSONArray jsonLevels, String playerId) throws JSONException {
			this.levels = new HashMap<>();
			
			for (int i = 0; i < jsonLevels.length(); i++) {
				JSONObject jsonLevel = jsonLevels.getJSONObject(i);
				int levelVal = jsonLevel.getInt("level");
				levels.put(playerId+levelVal, new Level(levelVal, jsonLevel.getInt("stars"), jsonLevel.getInt("score")));
			}
		}

		public LevelsSet(JSONObject jsonLevel, String playerId) throws JSONException {
			levels = new HashMap<>();
			int levelVal = jsonLevel.getInt("level");
			levels.put(playerId+levelVal, new Level(levelVal, jsonLevel.getInt("stars"), jsonLevel.getInt("score")));
		}
		
		public Level getLevel(String playerId, int level) {
			return levels.get(playerId+level);
		}
	}
	
	private LevelsSet levelsSet;
	private List<SqlUpdate> updateRequests = new ArrayList<>();
	private List<Row> insertData = new ArrayList<>();
	
	public LevelsUpdate(JSONArray jsonLevels, List<Row> exsistingLevels, String playerId, String levelsTableName) throws JSONException {
		levelsSet = new LevelsSet(jsonLevels, playerId);
		
		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "playerId", playerId)));
		
		for(Row row : exsistingLevels) {
			Level l = levelsSet.getLevel(playerId, (int)row.get("level"));
			if(l != null) {
				Row updatesData = new Row();
				updatesData.addCell(new CellData(Types.INTEGER, "score", l.getScore()));
				updatesData.addCell(new CellData(Types.INTEGER, "stars", l.getStars()));
				updateRequests.add(new SqlUpdate(levelsTableName, updatesData, where));
			} else {
				Row newInsertData = new Row();
				newInsertData.addCell(new CellData(Types.INTEGER, "score", l.getScore()));
				newInsertData.addCell(new CellData(Types.INTEGER, "stars", l.getStars()));
				insertData.add(newInsertData);
			}
		}
		
	}

	public LevelsUpdate(JSONObject jsonLevel, Row exsistingLevel, String playerId, String levelsTableName) throws JSONException {
		levelsSet = new LevelsSet(jsonLevel, playerId);

		SqlExpression where = new SqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "playerId", playerId)));
		
		if(levelsSet.getLevel(playerId, (int)exsistingLevel.get("level")) != null) {
			
		} else {
			
		}
	}
}
