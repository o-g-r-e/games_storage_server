package com.cm.dataserver.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;

import com.cm.dataserver.OAuthAccessToken;
import com.sun.mail.smtp.SMTPTransport;

public class EmailSender {
	
    private String smtpServer;
    //private String port;
    private String userName;
    private OAuthAccessToken oauthAccessToken;
    private String accessTokenFilePath;
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthRefreshToken;
    private String oauthTokenUrl;
    private String emailFrom;
    private boolean enabled;
    private Properties prop;
    private LogManager logManager;
    
    public EmailSender(String smtpServer, String userName, String oauthClientId, String oauthClientSecret, String oauthRefreshToken, String oauthTokenUrl, String accessTokenFilePath, String emailFrom, LogManager logManager) {
		
    	this.smtpServer = smtpServer;
    	//this.port = "465";
    	this.userName = userName;
    	this.oauthClientId = oauthClientId;
    	this.oauthClientSecret = oauthClientSecret;
    	this.oauthRefreshToken = oauthRefreshToken;
    	this.oauthTokenUrl = oauthTokenUrl;
    	this.accessTokenFilePath = accessTokenFilePath;
    	this.emailFrom = emailFrom;
    	this.logManager = logManager;
    	
    	prop = new Properties();
    	/*prop.put("mail.smtp.host", this.smtpServer);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", port);
        prop.put("mail.smtp.from", this.emailFrom);
        prop.put("mail.smtp.socketFactory.port", "25");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.socketFactory.fallback", "false");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.socketFactory.fallback", "true");*/
    	prop.put("mail.smtp.ssl.enable", "true");
    	prop.put("mail.smtp.sasl.enable", "true");
    	prop.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
    	prop.put("mail.smtp.auth.login.disable", "true");
    	prop.put("mail.smtp.auth.plain.disable", "true");
    }
    
	private void send(String emailTo, String subject, String content) throws MessagingException, MalformedURLException, IOException, JSONException {
		
		
		if(oauthAccessToken == null || oauthAccessToken.getAccessToken() == null) {
			oauthAccessToken = readAccessToken(accessTokenFilePath);
		}
		
		StringBuilder logContent = new StringBuilder();
		
		if(oauthAccessToken.expire()) {
			logContent.append("Access token expired, get new token...\n");
			
			String request = "client_id=" + URLEncoder.encode(oauthClientId, "UTF-8")
            	+ "&client_secret=" + URLEncoder.encode(oauthClientSecret, "UTF-8")
            	+ "&refresh_token=" + URLEncoder.encode(oauthRefreshToken, "UTF-8")
            	+ "&grant_type=refresh_token";
			 
			HttpURLConnection conn = (HttpURLConnection) new URL(oauthTokenUrl).openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
	       
			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print(request);
			out.flush();
			out.close();
			conn.connect();
	       
			String result = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining("\n"));
			logContent.append("Response from ").append(oauthTokenUrl).append(": ").append(result).append("\n");
			JSONObject jsonResult = new JSONObject(result);
			oauthAccessToken.setAccessToken(jsonResult.getString("access_token"));
			oauthAccessToken.setGenerationTimestamp(System.currentTimeMillis());
			oauthAccessToken.setExpirationPeriod((long) (jsonResult.getInt("expires_in") * 1000));
			logContent.append("New token recived: ").append(oauthAccessToken.getAccessToken()).append("\n");
			writeAccessToken(accessTokenFilePath, oauthAccessToken);
		}
		
		Session session = Session.getInstance(prop);
		
		Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailFrom));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailTo));
        message.setSubject(subject); 
        message.setSentDate(new Date());
        message.setText(content);
        message.saveChanges();
        
        Transport transport = session.getTransport("smtp");
        transport.connect(smtpServer, userName, oauthAccessToken.getAccessToken());
        transport.sendMessage(message, message.getAllRecipients());

        transport.close();
        
        logContent.append("Content: ").append(content).append("\n");
        logContent.append("Mail sent to ").append(emailTo).append("\n\n");
        logManager.log("mail", logContent.toString());
	}
	
	public void asyncSend(String emailTo, String subject, String content) {
		if(!enabled) return;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					send(emailTo, subject, content);
				} catch (MessagingException | IOException | JSONException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void enable(boolean enable) {
		this.enabled = enable;
	}
	
	private OAuthAccessToken readAccessToken(String fileName) {
		FileInputStream fileInputStream = null;
		ObjectInputStream objectInputStream = null;
		
		OAuthAccessToken result = OAuthAccessToken.getInstatnce();
		
		try {
			fileInputStream = new FileInputStream(fileName);
			objectInputStream = new ObjectInputStream(fileInputStream);
			result = (OAuthAccessToken) objectInputStream.readObject();
		} catch (IOException e) {
			printExceptionToFile(e);
		} catch (ClassNotFoundException e) {
			printExceptionToFile(e);
		} finally {
			try {
				if(fileInputStream != null) fileInputStream.close();
				if(objectInputStream != null) objectInputStream.close();
			} catch (IOException e1) {
				printExceptionToFile(e1);
			}
		}
		
		return result;
	}
	
	private void writeAccessToken(String fileName, OAuthAccessToken accessToken) throws IOException {
		FileOutputStream fileOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		
		File outputFile = new File(fileName);
		if(!outputFile.exists()) outputFile.createNewFile();
		
		try {
			fileOutputStream = new FileOutputStream(fileName);
			objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(accessToken);
		} catch (FileNotFoundException e) {
			printExceptionToFile(e);
		} catch (IOException e) {
			printExceptionToFile(e);
		} finally {
			try {
				if(fileOutputStream != null) fileOutputStream.close();
			} catch (IOException e1) {
				printExceptionToFile(e1);
			}
		}
	}
	
	private void printExceptionToFile(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		logManager.log("mail", sw.toString());
	}
}
