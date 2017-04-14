package com.customerservice.outbound.exception;

/**
 * Custom Exception for Converting Errors
 * @author Tim Dekarz
 */
public class ConvertingException extends Exception {

	public ConvertingException() {
		super();
	}

	public ConvertingException(String msg) {
		super(msg);
	}
}
