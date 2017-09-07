package com.customerservice.outbound.domain;

import lombok.Data;
import lombok.Value;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormData {

	private String title;

	private String firstName;

	private String lastName;

	private String email;

	private String password;

	private String taskId;

	private String contractId;

	private String subject;

	private String message;

	private List<MultipartFile> uploads;

	public FormData() {
		this.title = "";
		this.firstName = "";
		this.lastName = "";
		this.email = "";
		this.password = "";
		this.taskId = "";
		this.contractId = "";
		this.subject = "";
		this.message = "";
		this.uploads = new ArrayList<>();
	}
}
