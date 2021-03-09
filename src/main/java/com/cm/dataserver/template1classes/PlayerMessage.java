package com.cm.dataserver.template1classes;

public class PlayerMessage {
	private String id;
	private String senderFacebookId;
	private String type;
	private String message;
	
	public PlayerMessage(String id, String senderFacebookId, String type, String message) {
		this.id = id;
		this.senderFacebookId = senderFacebookId;
		this.type = type;
		this.message = message;
	}
	
	public String getId() {
		return id;
	}
	
	public String getSenderFacebookId() {
		return senderFacebookId;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
