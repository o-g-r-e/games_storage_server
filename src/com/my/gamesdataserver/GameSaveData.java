package com.my.gamesdataserver;
import java.util.ArrayList;
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
}
