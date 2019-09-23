package com.my.gamesdataserver;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SqlInsert extends AbstractSqlRequest {
	

	private List<List<DataCell>> data = new ArrayList<List<DataCell>>();
	
	public SqlInsert(HttpRequest httpRequest) throws JSONException {
		super(httpRequest);
		if(httpRequest.getContent() != null) {
			JSONArray rootArray = new JSONArray(httpRequest.getContent());
			for (int i = 0; i < rootArray.length(); i++) {
				JSONArray jsonRow = (JSONArray) rootArray.get(i);
				data.add(AbstractSqlRequest.dataCellRow(jsonRow));
			}
		}
	}

	@Override
	public boolean validate() {
		return parameters.containsKey("game_api_key") && parameters.containsKey("player_id");
	}

	public List<List<DataCell>> getData() {
		return data;
	}
}
