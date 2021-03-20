package com.cm.dataserver.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogManager {
	
	private static ExecutorService executorService;
	private static String logsDirectoryPath;
	private static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
	private static String logTemplate = "[%s]:%s";
	
	class LogService implements Runnable {
		private String prefix;
		private String logPart;
		LogService(String prefix, String logMessage) {
			this.prefix = prefix;
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

	public void log(String filePrefix, String inputRequest, String logContent, String response) {
		executorService.submit(new LogService(filePrefix,  String.format(logTemplate, currentDateTime(), "\n"+inputRequest+"\n\n"+logContent+"\n\n"+response+"\n\n")));
	}

	public void log(String filePrefix, String inputRequest, String logContent) {
		executorService.submit(new LogService(filePrefix,  String.format(logTemplate, currentDateTime(), "\n"+inputRequest+"\n\n"+logContent+"\n\n")));
	}
	
	public void log(String filePrefix, String logContent) {
		executorService.submit(new LogService(filePrefix,  String.format(logTemplate, currentDateTime(), "\n"+logContent+"\n\n")));
	}
	
	private String currentDateTime() {
		Date date = new Date(System.currentTimeMillis());
		return formatter.format(date);
	}
}
