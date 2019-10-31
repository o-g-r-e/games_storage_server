package com.my.gamesdataserver.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogManager {
	
	private ExecutorService executorService;
	private String logsDirectoryPath;
	
	class LogService implements Runnable {
		private String userId;
		private String logPart;
		LogService(String userId, String logMessage) {
			this.userId = userId;
			this.logPart = logMessage;
		}
		
		@Override
		public void run() {
			
			//BufferedWriter out = null;
			try {
				File logFile = new File(logsDirectoryPath+File.separator+userId+"_log.txt");
				if(!logFile.exists()) {
					logFile.createNewFile();
				}
				Files.write(Paths.get(logFile.getAbsolutePath()), logPart.getBytes(), StandardOpenOption.APPEND);
				//out = new BufferedWriter(new FileWriter(logFile)/*, 32768*/);
				//out.write(logPart);
			} catch (IOException e) {
				e.printStackTrace();
			} /*finally {
				if(out != null) {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}*/
		}
	}
	
	public LogManager(int poolCapacity) throws IOException {
		executorService = Executors.newFixedThreadPool(poolCapacity);
		logsDirectoryPath = new File(".").getCanonicalPath()+File.separator+"logs";
		File logsDirectory = new File(logsDirectoryPath);
		boolean isLogsDir = logsDirectory.exists()?true:logsDirectory.mkdirs();
	}

	public void log(String filePrefix, String input, String errorMessage, String output) {
		executorService.submit(new LogService(filePrefix,  input+"\n\n"+errorMessage+"\n\n"+output+"\n\n"));
	}
}
