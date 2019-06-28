package com.my.gamesdataserver;

public class DataBaseConnectionParameters {
	private String schema;
	private String host;
	private String port;
	private String dataBaseName;
	private String user;
	private String password;
	
	public DataBaseConnectionParameters(String schema, String host, String port, String dataBaseName, String user, String password) {
		this.schema = schema;
		this.host = host;
		this.port = port;
		this.dataBaseName = dataBaseName;
		this.user = user;
		this.password = password;
	}

	public String getSchema() {
		return schema;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	public String getDataBaseName() {
		return dataBaseName;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
	
	public String getUrl() {
		return schema+"://"+host+":"+port+"/"+dataBaseName+"?autoReconnect=true&useSSL=false";
	}
}
