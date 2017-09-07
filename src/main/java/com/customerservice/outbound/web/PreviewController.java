package com.customerservice.outbound.web;

import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.service.GenerationService;
import com.customerservice.outbound.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_PDF;

@Controller
public class PreviewController {

	@Value("${process.validateInput}")
	boolean validateInput;

	@Value("${file.output}")
	private String output;

	@Value("${preview.name}")
	private String previewName;

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
			File letter = new File(output + fileName + "/" + previewName + ".pdf");
			if (!letter.exists())
				return ResponseEntity.badRequest().build();

			return new ResponseEntity<>(Files.readAllBytes(letter.toPath()), prepareHttpHeaders(letter), HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	@RequestMapping(value = "/preview", method = RequestMethod.POST)
	public String generatePreview(@ModelAttribute FormData data, Map<String, Object> model) throws Exception {
		if (!validationService.handleValidation(data, model))
			return "form";

		File letter = generationService.generateLetter(data, previewName);
		return "redirect:/preview?id=" + letter.getParentFile().getName();
	}

	private HttpHeaders prepareHttpHeaders(File letter) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_PDF);
		String filename = previewName + ".pdf";
		headers.setContentDispositionFormData(filename, filename);
		headers.setContentLength(letter.length());
		headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		return headers;
	}
}
