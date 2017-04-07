package com.generali.outbound.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by timdekarz on 07.04.17.
 */
@Getter
@Setter
public class Location {

	private String location;

	public Location() {
		this.location = "";
	}
}
