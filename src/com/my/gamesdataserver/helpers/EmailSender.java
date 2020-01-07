package com.my.gamesdataserver.helpers;

import java.util.Date;
import java.util.Objects;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;

public class EmailSender {
	
    private String SMTP_SERVER;
    private String USERNAME;
    private String PASSWORD;
    private String EMAIL_FROM;
    private boolean enabled;
    private Properties prop;
    private Session session;
    private Message message;
    
    public EmailSender(String smtpServer, String user, String password, String emailFrom) {
		
    	Objects.requireNonNull(smtpServer);
		Objects.requireNonNull(user);
		Objects.requireNonNull(password);
		Objects.requireNonNull(emailFrom);
		
    	this.SMTP_SERVER = smtpServer;
    	this.USERNAME = user;
    	this.PASSWORD = password;
    	this.EMAIL_FROM = emailFrom;
    	
    	prop = new Properties();
    	prop.put("mail.smtp.host", SMTP_SERVER);
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", "25");
        prop.put("mail.smtp.from", EMAIL_FROM);
        prop.put("mail.smtp.socketFactory.port", "25");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.socketFactory.fallback", "false");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.enable", "false");
        prop.put("mail.smtp.socketFactory.fallback", "true");
        
        session = Session.getInstance(prop, null);
        message = new MimeMessage(session);
    }
    
	private void sendTo(String emailTo, String subject, String content) throws MessagingException {
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false));
        //message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("", false));
        message.setSubject(subject); 
        message.setText(content);
        message.setSentDate(new Date());
        SMTPTransport smtpTransport = (SMTPTransport) session.getTransport("smtp");
        smtpTransport.connect(SMTP_SERVER, USERNAME, PASSWORD);
        smtpTransport.sendMessage(message, message.getAllRecipients());

        //System.out.println("Response: " + t.getLastServerResponse());

        smtpTransport.close();
	}
	
	public void send(String emailTo, String subject, String content) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if(enabled) sendTo(emailTo, subject, content);
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void enable(boolean enable) {
		this.enabled = enable;
	}
}
