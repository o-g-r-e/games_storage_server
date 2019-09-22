package com.my.gamesdataserver;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateRequest extends AbstractRequest {
	private List<DataCell> set = new ArrayList<DataCell>();
	private List<DataCell> where = new ArrayList<DataCell>();
	
	public UpdateRequest(HttpRequest httpRequest) throws JSONException {
		super(httpRequest);
		
		if(httpRequest.getContent() != null) {
			JSONObject rootObject = new JSONObject(httpRequest.getContent());
			for (int i = 0; i < rootObject.length(); i++) {
				set = AbstractRequest.dataCellRow((JSONArray) rootObject.get("set"));
				where = AbstractRequest.dataCellRow((JSONArray) rootObject.get("where"));
			}
		}
	}
	
	@Override
	public boolean validate() {
		return true;
	}

	public List<DataCell> getSet() {
		return set;
	}

	public List<DataCell> getWhere() {
		return where;
	}
}
