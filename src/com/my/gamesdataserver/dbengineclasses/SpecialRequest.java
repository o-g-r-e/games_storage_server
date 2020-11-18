package com.my.gamesdataserver.dbengineclasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.my.gamesdataserver.basedbclasses.queryclasses.Helpers;
import com.my.gamesdataserver.basedbclasses.queryclasses.Select;
import com.my.gamesdataserver.basedbclasses.queryclasses.SqlExpression;

public class SpecialRequest {
	private int gameId;
	private String requestName;
	/*private String table;
	private String fields;
	private List<String> fieldsList;*/
	private Select request;
	
	public SpecialRequest(int gameId, String requestName, String table, String fields) {
		this.gameId = gameId;
		this.requestName = requestName;
		/*this.table = table;
		this.fields = fields;
		fieldsList = new ArrayList<>();
		String[] fieldsArray = fields.split(",");
		for(String fieldValue : fieldsArray) {
			fieldsList.add(fieldValue.trim());
		}*/
		request = new Select(table, fields);
	}

	public int getGameId() {
		return gameId;
	}

	public String getRequestName() {
		return requestName;
	}

	public String getTable() {
		return request.getTable();
	}

	public SpecialRequest setTable(String name) {
		request.setTable(name);
		return this;
	}

	public String getFields() {
		return request.getFields();
	}
	
	public SqlExpression getWhere() {
		return request.getWhere();
	}

	@Override
	public String toString() {
		return request.toString();
	}
	
	public SpecialRequest setWhere(JSONArray jsonWhere) throws JSONException {
		request.setWhere(jsonWhere);
		return this;
	}

	/*public List<String> getFieldsList() {
		return fieldsList;
	}*/
}
