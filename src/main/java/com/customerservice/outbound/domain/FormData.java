package com.customerservice.outbound.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Class for User Input from Webform
 * @author Tim Dekarz
 */
@Getter
@Setter
public class FormData {

	private String title; //should be 'male' or 'female'

	private String firstName;

	private String lastName;

	private String email;

	private String password;

	private String taskId; //should only be digits

	private String contractId; //should only be digits

	private String subject;

	private String message;

	private List<MultipartFile> uploads; //use multipartfile for spring upload support

	/**
	 * Standard constructur to ensure default values
	 */
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
