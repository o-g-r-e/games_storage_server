package com.my.gamesdataserver;

public class SqlSelect extends AbstractSqlRequest {

	SqlSelect(HttpRequest httpRequest) {
		super(httpRequest);
		
	}

	@Override
	public boolean validate() {
		return true;
	}

}
