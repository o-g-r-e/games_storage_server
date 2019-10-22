package com.my.gamesdataserver.match3dbclasses;

import java.util.List;

public class Match3PlayerData {
	private int maxLevel;
	/*private Match3PlayerData.PlayerLevel[] levels;
	private Match3PlayerData.PlayerBoost[] boosts;*/
	private List<Match3Level> levels;
	private List<Match3Boost> boosts;
	
	public Match3PlayerData(int maxLevel, List<Match3Level> levels, List<Match3Boost> boosts) {
		this.maxLevel = maxLevel;
		this.levels = levels;
		this.boosts = boosts;
	}



	/*private static class PlayerLevel {
		private int scores;
		private int stars;
		
		public PlayerLevel(int scores, int stars) {
			this.scores = scores;
			this.stars = stars;
		}
		
		public int getScores() {
			return scores;
		}
		
		public int getStars() {
			return stars;
		}
	}
	
	private static class PlayerBoost {
		private String name;
		private int count;
		
		public PlayerBoost(String name, int count) {
			this.name = name;
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public int getCount() {
			return count;
		}
	}*/
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		StringBuilder levelsJson = new StringBuilder("[");
		
		for (int i = 0; i < levels.size(); i++) {
			Match3Level level = levels.get(i);
			
			levelsJson.append("{\"scores\":").append(level.getScore());
			levelsJson.append(",\"stars\":").append(level.getStars()).append("}");
			
			if(i < levels.size()-1) {
				levelsJson.append(",");
			}
		}
		
		levelsJson.append("]");
		
		StringBuilder boostsJson = new StringBuilder("[");
		
		for (int i = 0; i < boosts.size(); i++) {
			Match3Boost boost = boosts.get(i);
			
			boostsJson.append("{\"name\":\"").append(boost.getName()).append("\"");
			boostsJson.append(",\"count\":").append(boost.getCount()).append("}");
			
			if(i < boosts.size()-1) {
				boostsJson.append(",");
			}
		}
		
		boostsJson.append("]");
		
		return "{\"max_level\":"+maxLevel+",\"levels\":"+levelsJson.toString()+",\"boosts\":"+boostsJson.toString()+"}";
	}
}
