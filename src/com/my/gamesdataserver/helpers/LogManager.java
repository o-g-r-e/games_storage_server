package com.my.gamesdataserver.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogManager {
	
	private static ExecutorService executorService;
	private static String logsDirectoryPath;
	
	class LogService implements Runnable {
		private String prefix;
		private String logPart;
		LogService(String userId, String logMessage) {
			this.prefix = userId;
			this.logPart = logMessage;
		}
		
		@Override
		public void run() {
			try {
				Files.write(Paths.get(logsDirectoryPath+File.separator+prefix+"_log.txt"), logPart.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public LogManager(int poolCapacity) throws IOException {
		executorService = Executors.newFixedThreadPool(poolCapacity);
		logsDirectoryPath = new File(".").getCanonicalPath()+File.separator+"logs";
		File logsDirectory = new File(logsDirectoryPath);
		if(!logsDirectory.exists()) logsDirectory.mkdirs();
	}

	public void log(String filePrefix, String input, String errorMessage, String output) {
		executorService.submit(new LogService(filePrefix,  input+"\n\n"+errorMessage+"\n\n"+output+"\n\n"));
	}

	public void log(String filePrefix, String input, String errorMessage) {
		executorService.submit(new LogService(filePrefix,  input+"\n\n"+errorMessage+"\n\n"));
	}
}
