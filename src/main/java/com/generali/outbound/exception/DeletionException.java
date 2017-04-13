package com.generali.outbound.exception;

/**
 * Custom Exception for Deletion Errors
 * @author Tim Dekarz
 */
public class DeletionException extends Exception {

	public DeletionException() {
		super();
	}

	public DeletionException(String msg) {
		super(msg);
	}
}
