package com.fincom.sanction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTenantException extends RuntimeException {

	public InvalidTenantException(String message) {
		super(message);
	}
}
