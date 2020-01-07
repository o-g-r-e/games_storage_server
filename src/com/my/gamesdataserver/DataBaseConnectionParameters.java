package com.my.gamesdataserver;

import java.util.Objects;

public class DataBaseConnectionParameters {
	private String schema;
	private String host;
	private String port;
	private String dataBaseName;
	private String user;
	private String password;
	
	public DataBaseConnectionParameters(String schema, String host, String port, String dataBaseName, String user, String password) {
		this.schema = Objects.requireNonNull(schema);
		this.host = Objects.requireNonNull(host);
		this.port = Objects.requireNonNull(port);
		this.dataBaseName = Objects.requireNonNull(dataBaseName);
		this.user = Objects.requireNonNull(user);
		this.password = Objects.requireNonNull(password);
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
	
	@Override
	public String toString() {
		return schema+"://"+host+":"+port+"/"+dataBaseName+"?autoReconnect=true&useSSL=false";
	}
}
