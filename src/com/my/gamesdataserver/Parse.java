package com.my.gamesdataserver;

import java.util.HashMap;
import java.util.Map;

public class Parse {
	public static Map<String, String> spltBy(String input, String chars) {
		Map<String, String> result = new HashMap<>();
		String[] s1 = input.split(chars);
		for (int i = 0; i < s1.length; i+=2) {
			result.put(s1[i], s1[i+1]);
		}
		return result;
	}
}
