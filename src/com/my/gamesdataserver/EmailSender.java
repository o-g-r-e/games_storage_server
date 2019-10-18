package com.my.gamesdataserver;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;

public class EmailSender {
	
    private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final String USERNAME = "info@candy-smith.com";
    private static final String PASSWORD = "666slayer333";

    private static final String EMAIL_FROM = "info@candy-smith.com";
    private static final String EMAIL_TO_CC = "";
    
	public static void sendTo(String emailTo, String subject, String content) throws MessagingException {
		Properties prop = System.getProperties();
        prop.put("mail.smtp.host", SMTP_SERVER); //optional, defined in SMTPTransport
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", "25"); // default port 25
        
        prop.put("mail.smtp.from", EMAIL_FROM);
        prop.put("mail.smtp.socketFactory.port", "25");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.socketFactory.fallback", "false");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.enable", "false");
        prop.put("mail.smtp.socketFactory.fallback", "true");

        Session session = Session.getInstance(prop, null);
        Message message = new MimeMessage(session);
        
			// from
            message.setFrom(new InternetAddress(EMAIL_FROM));

			// to 
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false));

			// cc
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(EMAIL_TO_CC, false));

			// subject
            message.setSubject(subject);
			
			// content 
            message.setText(content);
			
            message.setSentDate(new Date());

			// Get SMTPTransport
            SMTPTransport smtpTransport = (SMTPTransport) session.getTransport("smtp");
			
			// connect
            smtpTransport.connect(SMTP_SERVER, USERNAME, PASSWORD);
			
			// send
            smtpTransport.sendMessage(message, message.getAllRecipients());

            //System.out.println("Response: " + t.getLastServerResponse());

            smtpTransport.close();
	}
	
	public static void send(String emailTo, String subject, String content) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					sendTo(emailTo, subject, content);
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
