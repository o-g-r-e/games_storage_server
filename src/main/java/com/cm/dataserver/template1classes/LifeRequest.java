package com.cm.dataserver.template1classes;

public class LifeRequest {
	private String id;
	private String lifeSenderId;
	private String lifeReceiverId;
	private String status;
	
	public LifeRequest(String id, String lifeSenderFacebookId, String lifeReceiverFacebookId, String status) {
		this.id = id;
		this.lifeSenderId = lifeSenderFacebookId;
		this.lifeReceiverId = lifeReceiverFacebookId;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public String getLifeSenderId() {
		return lifeSenderId;
	}

	public String getLifeReceiverId() {
		return lifeReceiverId;
	}

	public String getStatus() {
		return status;
	}
}
