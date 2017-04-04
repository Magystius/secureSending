package com.generali.outbound.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by timdekarz on 29.03.17.
 */
@Getter
@Setter
public class FormData {

	private String title;

	private String firstName;

	private String lastName;

	private String email;

	private String password;

	private boolean sms;

	private String telephone;

	private String task;

	private String insuranceId;

	private String message;

	private List<MultipartFile> uploads;

	public FormData() {
		this.title = "";
		this.firstName = "";
		this.lastName = "";
		this.email = "";
		this.password = "";
		this.sms = false;
		this.telephone = "";
		this.task = "";
		this.insuranceId = "";
		this.message = "";
		this.uploads = new ArrayList<>();
	}
}
