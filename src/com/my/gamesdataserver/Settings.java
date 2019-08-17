package com.my.gamesdataserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Settings {
	private String serverPort;
	private String dbAddr;
	private String dbPort;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	public static final String defaultSettingsFilePath = "settings"+File.separator+"settings.txt";
	
	public void readSettings(File settingsFile) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(settingsFile);
			while (scanner.hasNextLine()) {
				parseEntry(scanner.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(scanner != null) {
				scanner.close();
			}
		}
	}
	
	private void parseEntry(String entry) {
		String[] data = entry.split("=");
		
		if(data.length < 2) {
			return;
		}
		
		switch (data[0]) {
		case "serverPort":
			serverPort = data[1];
			break;
		case "dbAddr":
			dbAddr = data[1];
			break;
		case "dbPort":
			dbPort = data[1];
			break;
		case "dbName":
			dbName = data[1];
			break;
		case "dbUser":
			dbUser = data[1];
			break;
		case "dbPassword":
			dbPassword = data[1];
			break;
		}
	}

	public String getServerPort() {
		return serverPort;
	}

	public String getDbAddr() {
		return dbAddr;
	}

	public String getDbPort() {
		return dbPort;
	}

	public String getDbName() {
		return dbName;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}
}
