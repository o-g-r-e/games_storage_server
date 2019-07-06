package com.my.gamesdataserver.dbmodels;

import org.json.JSONException;
import org.json.JSONObject;

public class SaveEntity {
	private int id;
	private int gameId;
	private int playerId;
	private String saveData;
	private String boostData;
	
	public SaveEntity(int id, int gameId, int playerId, String saveData, String boostData) {
		this.id = id;
		this.gameId = gameId;
		this.playerId = playerId;
		this.saveData = saveData;
		this.boostData = boostData;
	}

	public int getId() {
		return id;
	}

	public int getGameId() {
		return gameId;
	}

	public int getPlayerId() {
		return playerId;
	}

	public String getSaveData() {
		return saveData;
	}

	public String getBoostData() {
		return boostData;
	}
	
	public String toJson()  {
		return "[ "+id+", "+gameId+", "+playerId+", \""+saveData+"\", \""+boostData.replaceAll("\"", "\\\"")+"\" ]";
	}
}
