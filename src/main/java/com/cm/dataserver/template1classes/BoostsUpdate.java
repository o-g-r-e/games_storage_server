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

public class BoostsUpdate {
	Map<String, String> existingLevelsMap = new HashMap<>();
	
	private List<String> updateRequests = new ArrayList<>();
	private StringBuilder insertRequest = new StringBuilder("INSERT INTO ");
	private boolean needInsert = false;
	
	public BoostsUpdate(JSONArray jsonBoosts, List<Row> exsistingBoosts, PlayerId playerId, String boostsTableName) throws JSONException {
		insertRequest.append(boostsTableName).append(" (").append(playerId.getFieldName()).append(", name, count) VALUES ");
		
		for(Row r : exsistingBoosts) {
			existingLevelsMap.put(r.getString("name"), r.getString("name"));
		}
		
		for(int i=0; i<jsonBoosts.length(); i++) {
			
			JSONObject inputJsonLevel = jsonBoosts.getJSONObject(i);
			
			String name = inputJsonLevel.getString("name");
			int count = inputJsonLevel.getInt("count");
			
			if(isLevelExists(name)) {
				updateRequests.add("UPDATE "+boostsTableName+" SET count="+count+" WHERE "+
				playerId.getFieldName()+"='"+playerId.getValue()+"' AND name='"+name+"'");
			} else {
				needInsert = true;
				if(insertRequest.charAt(insertRequest.length()-1) == ')') {
					insertRequest.append(",");
				}
				insertRequest.append("('"+playerId.getValue()+"',").append("'"+name+"',").append(count).append(")");
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
