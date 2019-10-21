package com.my.gamesdataserver.apisqlrequest;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.my.gamesdataserver.HttpRequest;
import com.my.gamesdataserver.rawdbmanager.CellData;

public abstract class AbstractSqlRequest {
	protected static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^\\/[\\w-]+\\/([\\w-]+)");
	private static final Pattern COMMAND_PATTERN = Pattern.compile("^\\/([^\\/]+)");
	protected Map<String, String> parameters;

	protected String tableName;
	
	public enum Type {INSERT_INTO_TABLE,
					  UPDATE_TABLE,
					  SELECT}
	
	public abstract boolean validate();
	
	AbstractSqlRequest(HttpRequest httpRequest) {
		parameters = httpRequest.getUrlParametrs();
		Matcher matcher = TABLE_NAME_PATTERN.matcher(httpRequest.getUrl());
		
		if(matcher.find()) {
			tableName = matcher.group(1);
		}
	}
	
	/*private String urlDecode(String parameterValue) {
		parameterValue = parameterValue.replaceAll("%20", " ");
		parameterValue = parameterValue.replaceAll("%22", "\"");
		return parameterValue;
	}*/
	
	public static AbstractSqlRequest.Type parseCommand(String urlRequest) {
		Matcher m = COMMAND_PATTERN.matcher(urlRequest);
		if(m.find()) {
			String command = m.group(1);
			switch (command) {
			case "insert_into":
					return Type.INSERT_INTO_TABLE;
			case "update":
					return Type.UPDATE_TABLE;
			case "select":
					return Type.SELECT;
			}
		}
			
		return null;
	}
	
	public static CellData parseCellData(String type, String name, String value) {
		switch (type) {
		case "INTEGER":
			return new CellData(Types.INTEGER, name, Integer.parseInt(value));
		case "STRING":
			return new CellData(Types.VARCHAR, name, value);
		case "FLOAT":
			return new CellData(Types.FLOAT, name, Float.parseFloat(value));
		}
		return null;
	}
	
	public static List<CellData> dataCellRow(JSONArray jsonArray) throws JSONException {
		List<CellData> result = new ArrayList<CellData>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject o = (JSONObject) jsonArray.get(i);
		
			String type = (String) o.get("type");
			String name = (String) o.get("name");
			String value = (String) o.get("value");
			
			result.add(AbstractSqlRequest.parseCellData(type, name, value));
		}
		return result;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public String getTableName() {
		return tableName;
	}
}
