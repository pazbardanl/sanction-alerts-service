package com.fincom.sanction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlertAlreadyDecidedException extends RuntimeException {

	public AlertAlreadyDecidedException(String message) {
		super(message);
	}
}
