package com.my.gamesdataserver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameSaveData {
	private List<Integer> stars;
	private String boostJsonData;
	
	public GameSaveData() {
		stars = new ArrayList<Integer>();
		boostJsonData = "";
	}
	
	public void addStars(Integer levelData) {
		stars.add(levelData);
	}
	
	public void setBoostData(String jsonData) {
		boostJsonData = jsonData;
	}

	public List<Integer> getLevelStars() {
		return stars;
	}

	public String getBoostData() {
		return boostJsonData;
	}
	
	public String toJsonString() {
		StringBuilder result = new StringBuilder();
		result.append("{ \"boosts\" : ");
		result.append(boostJsonData);
		result.append(", \"stars\" : [");
		for (int i = 0; i < stars.size(); i++) {
			result.append(stars.get(i));
			if(i < stars.size() - 1) {
				result.append(", ");
			}
		}
		result.append("]}");
		
		return result.toString();
	}
}
