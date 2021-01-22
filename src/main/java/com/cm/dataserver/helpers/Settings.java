package com.cm.dataserver.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Settings {
	private Map<String, String> parameters = new HashMap<>();
	
	public Settings(File settingsFile) throws FileNotFoundException {
		Scanner scanner = null;
		try {
			scanner = new Scanner(settingsFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] s = line.split("=");
				if("".equals(line) || s.length < 2) continue;
				parameters.put(s[0], s[1]);
			}
		} finally {
			if(scanner != null) {
				scanner.close();
			}
		}
	}

	public String get(String parameterName) {
		return parameters.get(parameterName)==null?"":parameters.get(parameterName);
	}
}
