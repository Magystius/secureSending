package com.customerservice.outbound.web;

import com.customerservice.outbound.service.ValidationService;
import com.customerservice.outbound.Utils;
import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.service.GenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * handles the preview functions
 * @author Tim Dekarz
 */
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

	/**
	 * recieve the generated pdf file via id
	 * @param fileName - id of file to get
	 * @return - response with pdf data
	 */
	@RequestMapping(value = "/preview", method = RequestMethod.GET)
	public ResponseEntity<byte[]> showPreview(@RequestParam(value = "id") String fileName) {

		try {
			// check if file exists
			File letter = new File(output + fileName + "/" + previewName + ".pdf");
			if(!letter.exists()) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			//prepare response
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/pdf"));
			String filename = previewName + ".pdf";
			headers.setContentDispositionFormData(filename, filename);
			headers.setContentLength(letter.length());
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return new ResponseEntity<>(Files.readAllBytes(letter.toPath()), headers, HttpStatus.OK); //read file and return it in response
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * upload and save a preview
	 * @param data - form data for pdf lettter
	 * @param model - model for web form
	 * @return - redirect url or form template
	 * @throws Exception
	 */
	@RequestMapping(value = "/preview", method = RequestMethod.POST)
	public String generatePreview(@ModelAttribute FormData data, Map<String, Object> model) throws Exception {

		//validation
		if(!validationService.handleValidation(data, model)) {
			return "form";
		}

		// generate the file
		File letter = generationService.generateLetter(data, previewName);

		//redirect to file
		return "redirect:/preview?id=" + letter.getParentFile().getName();
	}
}
