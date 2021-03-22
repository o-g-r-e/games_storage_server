package com.cm.databaseserver.exceptions;

public class AuthorizationException extends Exception {
	public AuthorizationException(String errorMessage) {
        super(errorMessage);
    }
}
