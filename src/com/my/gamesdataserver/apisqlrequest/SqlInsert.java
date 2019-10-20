package com.my.gamesdataserver.apisqlrequest;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.HttpRequest;
import com.my.gamesdataserver.rawdbmanager.CellData;

public class SqlInsert extends AbstractSqlRequest {
	

	private List<List<CellData>> data = new ArrayList<List<CellData>>();
	
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

	public List<List<CellData>> getData() {
		return data;
	}
}
