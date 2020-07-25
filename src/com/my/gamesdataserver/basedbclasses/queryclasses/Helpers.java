package com.my.gamesdataserver.basedbclasses.queryclasses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Helpers {
	public static SqlExpression jsonWhereToObject(JSONArray jsonWhere) throws JSONException {
		SqlExpression resultExp = null;
		for (int i = jsonWhere.length()-1; i >= 0; i--) {
			JSONArray orExpressions = jsonWhere.getJSONArray(i);
				
			SqlExpression exp = null;
			if(orExpressions.length() < 1) continue;
				
			if(orExpressions.length() == 1) {
				exp = new SimpleSqlExpression(orExpressions.getJSONObject(0).getString("name"), orExpressions.getJSONObject(0).get("value"));
			} else {
					
				JSONObject subLastObject = orExpressions.getJSONObject(orExpressions.length()-2);
				JSONObject lastObject = orExpressions.getJSONObject(orExpressions.length()-1);
				
				SimpleSqlExpression lExp = new SimpleSqlExpression(subLastObject.getString("name"), subLastObject.get("value"));
				SimpleSqlExpression rExp = new SimpleSqlExpression(lastObject.getString("name"), lastObject.get("value"));
				exp = new SqlLogicExpression(lExp, "AND", rExp);
					
				if(orExpressions.length() >= 3 ) {
					for (int j = orExpressions.length()-3; j <= 0; j--) {
						JSONObject andExpression = orExpressions.getJSONObject(j);
						SimpleSqlExpression curExp = new SimpleSqlExpression(andExpression.getString("name"), andExpression.get("value"));
						exp = new SqlLogicExpression(curExp, "AND", exp);
					}
				}
			}
			
			if(resultExp == null) {
				resultExp = exp;
			} else {
				resultExp = new SqlLogicExpression(exp, "OR", resultExp);
			}
		}
			
		return resultExp;
	}
}
