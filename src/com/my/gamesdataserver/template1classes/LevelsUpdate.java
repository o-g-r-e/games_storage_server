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
import com.my.gamesdataserver.basedbclasses.SqlMethods;
import com.my.gamesdataserver.basedbclasses.queryclasses.SimpleSqlExpression;
import com.my.gamesdataserver.dbengineclasses.PlayerId;

public class LevelsUpdate {
	private Map<String, Level> levels;
	
	private Map<String, Level> jsonLevelsToMap(JSONArray jsonLevels, String playerId) throws JSONException {
		Map<String, Level> result = new HashMap<>();
		
		for (int i = 0; i < jsonLevels.length(); i++) {
			JSONObject jsonLevel = jsonLevels.getJSONObject(i);
			int levelVal = jsonLevel.getInt("level");
			result.put(playerId+levelVal, new Level(playerId, levelVal, jsonLevel.getInt("score"), jsonLevel.getInt("stars")));
		}
		
		return result;
	}
	
	private List<String> updateRequests = new ArrayList<>();
	private StringBuilder insertRequest = new StringBuilder("INSERT INTO ");
	private boolean needInsert = false;
	
	public LevelsUpdate(JSONArray jsonLevels, List<Row> exsistingLevels, PlayerId playerId, String levelsTableName) throws JSONException {
		levels = jsonLevelsToMap(jsonLevels, playerId.getValue());
		insertRequest.append(levelsTableName).append(" (").append(playerId.getFieldName()).append(", level, score, stars) VALUES ");
		
		for(Map.Entry<String, Level> inputLevelEntry : levels.entrySet()) {
			
			Level inputLevel = inputLevelEntry.getValue();
			boolean levelExists = false;
			
			for(Row r : exsistingLevels) {
				if(((Integer)r.get("level")) == inputLevel.getLevel()) {
					levelExists = true;
					break;
				}
			}
			
			if(levelExists) {
				updateRequests.add("UPDATE "+levelsTableName+" SET score="+inputLevel.getScore()+", stars="+inputLevel.getStars()+" WHERE "+
				playerId.getFieldName()+"='"+playerId.getValue()+"' AND level="+inputLevel.getLevel());
			} else {
				needInsert = true;
				if(insertRequest.charAt(insertRequest.length()-1) == ')') {
					insertRequest.append(",");
				}
				insertRequest.append("(")
				.append("'").append(playerId.getValue()).append("'").append(",")
				.append(inputLevel.getLevel()).append(",")
				.append(inputLevel.getScore()).append(",")
				.append(inputLevel.getStars()).append(")");
			}
		}
	}
	
	public boolean isNeedInsert() {
		return needInsert;
	}
	
	public List<String> getUpdateRequests() {
		return updateRequests;
	}

	public String getInsertRequest() {
		return insertRequest.toString();
	}

	/*public LevelsUpdate(JSONObject jsonLevel, Row exsistingLevel, String playerId, String levelsTableName) throws JSONException {
		levelsSet = new LevelsSet(jsonLevel, playerId);

		SimpleSqlExpression where = new SimpleSqlExpression();
		where.addValue(new SqlWhereValue(new CellData(Types.VARCHAR, "playerId", playerId)));
		
		if(levelsSet.getLevel(playerId, (int)exsistingLevel.get("level")) != null) {
			
		} else {
			
		}
	}*/
}
