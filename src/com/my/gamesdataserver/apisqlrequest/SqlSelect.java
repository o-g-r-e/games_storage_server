package com.my.gamesdataserver.apisqlrequest;

import com.my.gamesdataserver.HttpRequest;

public class SqlSelect extends AbstractSqlRequest {

	public SqlSelect(HttpRequest httpRequest) {
		super(httpRequest);
		
	}

	@Override
	public boolean validate() {
		return true;
	}

}
