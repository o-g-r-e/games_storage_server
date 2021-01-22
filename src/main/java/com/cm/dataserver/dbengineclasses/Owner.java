package com.cm.dataserver.dbengineclasses;

public class Owner {
	private int id;
	private String email;
	
	public Owner(int id, String email) {
		this.id = id;
		this.email = email;
	}
	
	public int getId() {
		return id;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String toJson() {
		StringBuilder result = new StringBuilder();
		return result.append("[").append(id).append(",\"").append(email).append("\"]").toString();
	}
}
