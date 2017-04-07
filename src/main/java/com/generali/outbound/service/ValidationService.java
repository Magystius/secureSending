package com.generali.outbound.service;

import com.generali.outbound.domain.FormData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by timdekarz on 29.03.17.
 */
@Service
public class ValidationService {

	private static String[] supportedFileTypes = {"pdf", "jpg", "jpeg", "png", "xls", "xlsx", "doc", "docx"};

	public List<Map<String, String>> validateInput(FormData data) {

		List<Map<String, String>> errors = new ArrayList<>();

		if(data.getTitle() == null || (!data.getTitle().equals("female ") && !data.getTitle().equals("male"))) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Ungültiger Titel");
			error.put("field", "title");
			errors.add(error);
		}
		if(data.getFirstName() == null || data.getFirstName().length() < 3 ||
			!data.getFirstName().matches("^[ A-z]+$")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Vorname muss aus mind. 3 Zeichen bestehen und darf nur Buchstaben bzw. Leerzeichen enthalten");
			error.put("field", "firstName");
			errors.add(error);
		}
		if(data.getLastName() == null || data.getLastName().length() < 3 ||
			!data.getLastName().matches("^[ A-z]+$")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Nachname muss aus mind. 3 Zeichen bestehen und darf nur Buchstaben bzw. Leerzeichen enthalten");
			error.put("field", "lastName");
			errors.add(error);
		}
		final Pattern VALID_EMAIL_ADDRESS_REGEX =
			Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
		if (data.getEmail() == null || !VALID_EMAIL_ADDRESS_REGEX.matcher(data.getEmail()).matches()) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Ungültige Mailadresse");
			error.put("field", "email");
			errors.add(error);
		}
		//TODO: this should be discussed on customer wishes
		if(data.getPassword() == null
			//|| !data.getPassword().matches("\\A(?=\\S*?[0-9])(?=\\S*?[a-z])(?=\\S*?[A-Z])(?=\\S*?[@#$%^&+=])\\S{8,}\\z")
			 ) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Das Passwort entspricht nicht den Anforderungen");
			error.put("field", "password");
			errors.add(error);
		}
		if(!data.getTask().matches("[0-9]+")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Die Vorgangsnummer darf nur Zahlen enthalten");
			error.put("field", "task");
			errors.add(error);
		}
		if(!data.getInsuranceId().matches("[0-9]+")) {
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Die Versicherungsnummer darf nur Zahlen enthalten");
			error.put("field", "insuranceId");
			errors.add(error);
		}

		//TODO: validate free text and subject!

		if(data.getUploads().size() != 1 && !data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			data.getUploads().forEach((v) -> {
				String[] temp = v.getOriginalFilename().split(".");
				if(supportedFileTypes.toString().contains(temp[temp.length - 1])) {
					HashMap<String, String> error = new HashMap<>();
					error.put("error", "Nicht unterstütztes Dateiformat: " + temp[temp.length - 1]);
					error.put("field", "uploads");
					errors.add(error);
				}
			});
		}

		return errors;
	}

}
