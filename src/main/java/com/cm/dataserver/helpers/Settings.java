package com.cm.dataserver.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Settings {
	private Map<String, String> parameters = new HashMap<>();
	public static String invoiceKey;
	
	public Settings(File settingsFile) throws FileNotFoundException {
		Scanner scanner = null;
		try {
			scanner = new Scanner(settingsFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] s = line.split("=");
				if("".equals(line) || line.startsWith("#") || s.length < 2) continue;
				parameters.put(s[0], s[1]);
			}
		} finally {
			if(scanner != null) {
				scanner.close();
			}
			
			if(parameters.containsKey("invoiceKey")) {
				invoiceKey = parameters.get("invoiceKey");
			}
		}
	}

	public String getString(String parameterName) {
		return parameters.get(parameterName)==null?"":parameters.get(parameterName);
	}

	public boolean getBool(String parameterName) {
		return "Yes".equals(getString(parameterName));
	}
}
