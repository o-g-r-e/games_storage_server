package com.cm.dataserver.template1classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.basedbclasses.Row;
import com.cm.dataserver.dbengineclasses.PlayerId;

public class LevelsUpdate {
	
	Map<String, Integer> existingLevelsMap = new HashMap<>();
	
	private List<String> updateRequests = new ArrayList<>();
	private StringBuilder insertRequest = new StringBuilder("INSERT INTO ");
	private boolean needInsert = false;
	
	public LevelsUpdate(JSONArray jsonLevels, List<Row> exsistingLevels, PlayerId playerId, String levelsTableName) throws JSONException {
		insertRequest.append(levelsTableName).append(" (").append(playerId.getFieldName()).append(", level, score, stars) VALUES ");
		
		for(Row r : exsistingLevels) {
			existingLevelsMap.put(r.getString("level"), r.getInt("level"));
		}
		
		for(int i=0; i<jsonLevels.length(); i++) {
			
			JSONObject inputJsonLevel = jsonLevels.getJSONObject(i);
			
			int levelValue = inputJsonLevel.getInt("level");
			int scoreValue = inputJsonLevel.getInt("score");
			int starsValue = inputJsonLevel.getInt("stars");
			
			if(isLevelExists(String.valueOf(levelValue))) {
				updateRequests.add("UPDATE "+levelsTableName+" SET score="+scoreValue+", stars="+starsValue+" WHERE "+
				playerId.getFieldName()+"='"+playerId.getValue()+"' AND level="+levelValue);
			} else {
				needInsert = true;
				if(insertRequest.charAt(insertRequest.length()-1) == ')') {
					insertRequest.append(",");
				}
				insertRequest.append("(")
				.append("'"+playerId.getValue()+"',")
				.append(levelValue).append(",")
				.append(scoreValue).append(",")
				.append(starsValue).append(")");
			}
		}
	}
	
	private boolean isLevelExists(String levelNumb) {
		return existingLevelsMap.containsKey(levelNumb);
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
}
