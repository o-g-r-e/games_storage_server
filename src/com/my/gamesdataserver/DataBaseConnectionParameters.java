package com.my.gamesdataserver;

public class DataBaseConnectionParameters {
	private String schema;
	private String host;
	private String port;
	private String dataBaseName;
	private String user;
	private String passwrod;
	
	public DataBaseConnectionParameters(String schema, String host, String port, String dataBaseName, String user, String passwrod) {
		this.schema = schema;
		this.host = host;
		this.port = port;
		this.dataBaseName = dataBaseName;
		this.user = user;
		this.passwrod = passwrod;
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

	public String getPasswrod() {
		return passwrod;
	}
}
