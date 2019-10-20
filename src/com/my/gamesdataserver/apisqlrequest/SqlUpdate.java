package com.my.gamesdataserver.apisqlrequest;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.HttpRequest;
import com.my.gamesdataserver.rawdbmanager.CellData;

public class SqlUpdate extends AbstractSqlRequest {
	private List<CellData> set = new ArrayList<CellData>();
	private List<CellData> where = new ArrayList<CellData>();
	
	public SqlUpdate(HttpRequest httpRequest) throws JSONException {
		super(httpRequest);
		
		if(httpRequest.getContent() != null) {
			JSONObject rootObject = new JSONObject(httpRequest.getContent());
			for (int i = 0; i < rootObject.length(); i++) {
				set = AbstractSqlRequest.dataCellRow((JSONArray) rootObject.get("set"));
				where = AbstractSqlRequest.dataCellRow((JSONArray) rootObject.get("where"));
			}
		}
	}
	
	@Override
	public boolean validate() {
		return true;
	}

	public List<CellData> getSet() {
		return set;
	}

	public List<CellData> getWhere() {
		return where;
	}
}
