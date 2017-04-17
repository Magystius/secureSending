package com.customerservice.outbound.service;

import com.customerservice.outbound.domain.FormData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service class for providing validation services
 * @author Tim Dekarz
 */
@Service
public class ValidationService {

	private static String[] supportedFileTypes = {"pdf", "jpg", "jpeg", "png", "xls", "xlsx", "doc", "docx", "ppt", "pptx"}; //list with all supported files

	/**
	 * validates given data and returns list with errors - if they were found
	 * @param data - data to process
	 * @return - list of errors -> empty if data ok
	 */
	public List<Map<String, String>> validateInput(FormData data) {

		List<Map<String, String>> errors = new ArrayList<>();

		//CHECK TITLE
		if(data.getTitle() == null || (!data.getTitle().equals("female") && !data.getTitle().equals("male"))) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Ungültiger Titel");
			error.put("field", "title");
			errors.add(error);
		}
		//CHECK FIRST NAME
		if(data.getFirstName() == null || data.getFirstName().length() < 3 ||
			!data.getFirstName().matches("^[ A-z]+$")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Vorname muss aus mind. 3 Zeichen bestehen und darf nur Buchstaben bzw. Leerzeichen enthalten");
			error.put("field", "firstName");
			errors.add(error);
		}
		//CHECK LAST NAME
		if(data.getLastName() == null || data.getLastName().length() < 3 ||
			!data.getLastName().matches("^[ A-z]+$")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Nachname muss aus mind. 3 Zeichen bestehen und darf nur Buchstaben bzw. Leerzeichen enthalten");
			error.put("field", "lastName");
			errors.add(error);
		}
		//CHECK MAIL
		final Pattern VALID_EMAIL_ADDRESS_REGEX =
			Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		if (data.getEmail() == null || !VALID_EMAIL_ADDRESS_REGEX.matcher(data.getEmail()).matches()) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Ungültige Mailadresse");
			error.put("field", "email");
			errors.add(error);
		}
		//CHECK PASSWORD
		//TODO: this should be discussed on customer wishes
		if(data.getPassword() == null
			//|| !data.getPassword().matches("\\A(?=\\S*?[0-9])(?=\\S*?[a-z])(?=\\S*?[A-Z])(?=\\S*?[@#$%^&+=])\\S{8,}\\z")
			 ) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Das Passwort entspricht nicht den Anforderungen");
			error.put("field", "password");
			errors.add(error);
		}
		//CHECK TASKID
		if(!data.getTaskId().matches("[0-9]+")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Die Vorgangsnummer darf nur Zahlen enthalten");
			error.put("field", "taskId");
			errors.add(error);
		}
		//CHECK INSURANCEID
		if(!data.getContractId().matches("[0-9]+")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Die Versicherungsnummer darf nur Zahlen enthalten");
			error.put("field", "contractId");
			errors.add(error);
		}

		//TODO: validate free text and subject!

		//CHECK UPLOADS	if present
		if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			for(MultipartFile file : data.getUploads()) {
				String[] temp = file.getOriginalFilename().split("\\.");
				if(!Arrays.toString(supportedFileTypes).contains(temp[temp.length - 1])) {
					HashMap<String, String> error = new HashMap<>();
					error.put("error", "Nicht unterstütztes Dateiformat: " + temp[temp.length - 1]);
					error.put("field", "uploads");
					errors.add(error);
				}
			};
		}

		return errors;
	}
}
