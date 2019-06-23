package com.my.gamesdataserver;

public class LevelData {
	private int levelNum;
	private int levelStars;
	
	public LevelData(int levelNum, int levelStars) {
		this.levelNum = levelNum;
		this.levelStars = levelStars;
	}

	public int getLevelNum() {
		return levelNum;
	}

	public int getLevelStars() {
		return levelStars;
	}
}
