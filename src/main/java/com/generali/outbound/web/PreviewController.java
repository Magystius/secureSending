package com.generali.outbound.web;

import com.generali.outbound.Utils;
import com.generali.outbound.domain.FormData;
import com.generali.outbound.service.GenerationService;
import com.generali.outbound.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Created by Tim on 12.04.2017.
 */
@Controller
public class PreviewController {

	private final ValidationService validationService;
	private final GenerationService generationService;

	@Autowired
	public PreviewController(final ValidationService validationService, final GenerationService generationService) {
		this.validationService = validationService;
		this.generationService = generationService;
	}

	@RequestMapping(value = "/preview", method = RequestMethod.GET)
	public ResponseEntity<byte[]> showPreview(@RequestParam(value = "id") String fileName) {

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

	@RequestMapping(value = "/preview", method = RequestMethod.POST)
	public String generatePreview(@ModelAttribute FormData data, Map<String, Object> model) throws Exception {

		//validation
		List<Map<String, String>> errors = validationService.validateInput(data);
		if (errors.size() > 0) {
			//invalid input
			Utils.populateModel(data, model);
			model.put("failure", true);
			model.put("errorList", errors);
		} else {
			// generate the file
			File letter = generationService.generateLetter(data);

			return "redirect:/preview?id=" + letter.getParentFile().getName();
		}

		return "form";
	}
}
