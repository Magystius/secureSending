package com.generali.outbound.web;

import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.generali.outbound.domain.FormData;
import com.generali.outbound.service.GenerationService;
import com.generali.outbound.service.ValidationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;

/**
 * Created by timdekarz on 27.03.17.
 */
@Controller
public class FormController {

	@Value("${customer}")
	private String customer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final ValidationService validationService;
	private final GenerationService generationService;

	@Autowired
	public FormController(final ValidationService validationService, final GenerationService generationService) {
		this.validationService = validationService;
		this.generationService = generationService;
	}

	@RequestMapping(value = "/form", method = RequestMethod.GET)
	public String initial(FormData data,
						  Map<String, Object> model) {

		logger.info("index route called");

		populateModel(data, model);

		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String submit(@ModelAttribute FormData data, Map<String, Object> model) {

		data.getUploads().forEach(v -> logger.info(v.getOriginalFilename()));
		//TODO: Delete MOCKUP
		//Pause for 4 seconds
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//validation
		List<Map<String, String>> errors = validationService.validateInput(data);
		if (errors.size() > 0) {
			//invalid input
			populateModel(data, model);
			model.put("failure", true);
			model.put("errorList", errors);
		} else {
			//TODO: do something with your pdfs
			populateModel(new FormData(), model);
			model.put("success", true);
		}

		return "form";
	}

	@RequestMapping(value = "/generatepdf", method = RequestMethod.POST)
	public ResponseEntity<byte[]> generatePDF(@ModelAttribute FormData data) {

		try {
			//validation
			List<Map<String, String>> errors = validationService.validateInput(data);
			if (errors.size() > 0) {
				//invalid input
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			} else {
				// generate the file
				File letter = generationService.generateLetter(data);

				//prepare response
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.parseMediaType("application/pdf"));
				String filename = "preview.pdf";
				headers.setContentDispositionFormData(filename, filename);
				headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

				return new ResponseEntity<>(Files.readAllBytes(letter.toPath()), headers, HttpStatus.OK);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Map<String, Object> populateModel(FormData data, Map<String, Object> model) {

		Boolean titleUnselected, titleFemale, titleMale;
		titleUnselected = !data.getTitle().equals("female") && !data.getTitle().equals("male");
		titleFemale = data.getTitle().equals("female");
		titleMale = data.getTitle().equals("male");
		model.put("titleUnselected", titleUnselected);
		model.put("titleFemale", titleFemale);
		model.put("titleMale", titleMale);

		model.put("firstName", data.getFirstName());
		model.put("lastName", data.getLastName());

		model.put("email", data.getEmail());
		model.put("tel", data.getTelephone());
		boolean telSelect = !(data.getTelephone() == null || data.getTelephone().isEmpty());
		model.put("telSelect", telSelect);

		model.put("task", data.getTask());
		model.put("insuranceId", data.getInsuranceId());

		model.put("success", false);
		model.put("failure", false);

		return model;
	}
}
