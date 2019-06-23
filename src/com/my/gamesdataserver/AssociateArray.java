package com.my.gamesdataserver;
import java.util.HashMap;
import java.util.Map;

public class AssociateArray {
	Map<String, Object> data = new HashMap<String, Object>();
	
	void addPair(String key, String value) {
		data.put(key, value);
	}
	
	void addAssArray(String name, AssociateArray array) {
		data.put(name, array.data);
	}
	
	String toString(int tabLevel) {
		StringBuilder result = new StringBuilder();
		StringBuilder tabs = new StringBuilder();
		result.append("{\n");
		for(int i = 0; i<tabLevel; i++) {
			tabs.append("\t");
		}
		
		for(Map.Entry<String, Object> e : data.entrySet()) {
			if(e.getValue().getClass() == HashMap.class) {
				result.append(tabs.toString()+"\""+e.getKey()+"\" : \""+((AssociateArray)e.getValue()).toString(tabLevel+1));
			} else if (e.getValue().getClass() == String.class) {
				result.append(tabs.toString()+"\""+e.getKey()+"\" : \""+e.getValue()+"\"");
			}
		}
		result.append("}");
		return result.toString();
	}
}
