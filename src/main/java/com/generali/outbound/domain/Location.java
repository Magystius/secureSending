package com.generali.outbound.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity Class for async callbacks of embedded images
 * @author Tim Dekarz
 */
@Getter
@Setter
public class Location {

	private String location; //should be relative path. pay attention for base urls

	/**
	 * Standard Constructor for default values
	 */
	public Location() {
		this.location = "";
	}
}
