package com.my.gamesdataserver.gamesdbmanager;

public class Match3PlayerData {
	private int maxLevel;
	private Match3PlayerData.PlayerLevel[] levels;
	private Match3PlayerData.PlayerBoost[] boosts;
	
	private static class PlayerLevel {
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
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		StringBuilder levelsJson = new StringBuilder("[");
		
		for (int i = 0; i < levels.length; i++) {
			Match3PlayerData.PlayerLevel level = levels[i];
			
			levelsJson.append("{\"scores\":").append(level.getScores());
			levelsJson.append(",\"stars\":").append(level.getStars()).append("}");
			
			if(i < levels.length-1) {
				levelsJson.append(",");
			}
		}
		
		levelsJson.append("]");
		
		StringBuilder boostsJson = new StringBuilder("[");
		
		for (int i = 0; i < boosts.length; i++) {
			Match3PlayerData.PlayerBoost boost = boosts[i];
			
			boostsJson.append("{\"name\":\"").append(boost.getName()).append("\"");
			boostsJson.append(",\"count\":").append(boost.getCount()).append("}");
			
			if(i < boosts.length-1) {
				boostsJson.append(",");
			}
		}
		
		boostsJson.append("]");
		
		return "{\"max_level\":"+maxLevel+",\"levels\":"+levelsJson.toString()+",\"boosts\":"+boostsJson.toString()+"}";
	}
}
