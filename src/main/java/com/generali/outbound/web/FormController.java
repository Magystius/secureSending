package com.generali.outbound.web;

import com.generali.outbound.domain.FormData;
import com.generali.outbound.domain.Location;
import com.generali.outbound.service.GenerationService;
import com.generali.outbound.service.ValidationService;
import com.itextpdf.text.DocumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
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
			try {
				List<File> files = new ArrayList<>();
				files.addAll(generationService.generateAll(data)); //use this method for parallel converting/generating
				generationService.mergeDir(data.getEmail()); // use mail as dir id to merge all previously generated docs
				//TODO: send to somewhere
				//generationService.deleteFiles(data.getEmail(), null); //delete temp files

			} catch (Exception e) {
				populateModel(data, model);
				model.put("failure", true);
				errors = new ArrayList<>();
				HashMap<String, String> error = new HashMap<>();
				error.put("error", "Bei der Generierung ist ein unerwarteter Fehler aufgetreten");
				errors.add(error);
				model.put("errorList", errors);
			}
			populateModel(new FormData(), model);
			model.put("success", true);
		}

		return "form";
	}

	@RequestMapping(value = "/generatepreview", method = RequestMethod.POST)
	public String generatePreview(@ModelAttribute FormData data, Map<String, Object> model) throws Exception {

		//validation
		List<Map<String, String>> errors = validationService.validateInput(data);
		if (errors.size() > 0) {
			//invalid input
			populateModel(data, model);
			model.put("failure", true);
			model.put("errorList", errors);
		} else {
			// generate the file
			File letter = generationService.generateLetter(data);

			return "redirect:/showpreview?file=" + letter.getParentFile().getName();
		}

		return "form";
	}

	@RequestMapping(value = "/showpreview", method = RequestMethod.GET)
	public ResponseEntity<byte[]> showPreview(@RequestParam(value = "file") String fileName) {

		try {
			// check if file exists
			File letter = new File("./tmp/" + fileName + "/preview.pdf");
			if(!letter.exists()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			//prepare response
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/pdf"));
			String filename = "preview.pdf";
			headers.setContentDispositionFormData(filename, filename);
			headers.setContentLength(letter.length());
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return new ResponseEntity<>(Files.readAllBytes(letter.toPath()), headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/saveimage", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody Location saveImage(@ModelAttribute MultipartFile file, HttpServletResponse response) throws Exception {

		String type;
		if(file.getContentType().contains("png")) {
			type = "png";
		} else if(file.getContentType().contains("jpg") || file.getContentType().contains("jpeg")) {
			type = "jpg";
		} else {
			response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
			return new Location();
		}
		String fileName = Long.toString(System.currentTimeMillis()) + "." + type;
		File psFile = new File("./img/" + fileName);
		if (!psFile.exists()) {
			psFile.createNewFile();
		}
		//byte[] imageBytes = Base64.getDecoder().decode(file.getBytes());
		new FileOutputStream(psFile).write(file.getBytes());

		Location loc = new Location();
		loc.setLocation(fileName);
		return loc;
	}

	@RequestMapping(value = "/getimage", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getImage(@RequestParam(value = "image") String fileName) {

		try {
			// check if file exists
			File image = new File("./img/" + fileName);
			if(!image.exists()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			//prepare response
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("image/jpeg"));
			headers.setContentLength(image.length());

			return new ResponseEntity<>(Files.readAllBytes(image.toPath()), headers, HttpStatus.OK);
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
		model.put("password", data.getPassword());

		model.put("task", data.getTask());
		model.put("insuranceId", data.getInsuranceId());

		model.put("subject", data.getSubject());
		model.put("message", data.getMessage());

		model.put("success", false);
		model.put("failure", false);

		return model;
	}
}
