package com.generali.outbound.web;

import com.generali.outbound.Utils;
import com.generali.outbound.domain.FormData;
import com.generali.outbound.service.ConvertingService;
import com.generali.outbound.service.GenerationService;
import com.generali.outbound.service.ValidationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

/**
 * Web Controller for the plain form
 * contains base url
 * @author Tim Dekarz
 */
@Controller
public class FormController {

	@Value("${customer}")
	private String customer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final ValidationService validationService;
	private final GenerationService generationService;
	private final ConvertingService convertingService;

	@Autowired
	public FormController(final ValidationService validationService, final GenerationService generationService,
						  final ConvertingService convertingService) {
		this.validationService = validationService;
		this.generationService = generationService;
		this.convertingService = convertingService;
	}

	/**
	 * serves initial form
	 * @param data - empty formdata object
	 * @param model - empty model
	 * @return
	 */
	@RequestMapping(value = "/form", method = RequestMethod.GET)
	public String initial(FormData data,
						  Map<String, Object> model) {

		logger.info("index route called");

		Utils.populateModel(data, model); //set values for view

		return "form";
	}

	/**
	 *
	 * @param data
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String submit(@ModelAttribute FormData data, Map<String, Object> model) {

		data.getUploads().forEach(v -> logger.info(v.getOriginalFilename()));
		//TODO: Delete MOCKUP
		//Pause for 4 seconds
		/*try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/

		//validation
		List<Map<String, String>> errors = validationService.validateInput(data);
		if (errors.size() > 0) {
			//invalid input
			Utils.populateModel(data, model);
			model.put("failure", true);
			model.put("errorList", errors);
		} else {
			//TODO: do something with your pdfs
			try {
				List<File> files = new ArrayList<>();
				files.addAll(generationService.processAll(data)); //use this method for parallel converting/generating
				/*files.add(generationService.generateLetter(data));
				if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
					for(MultipartFile file : data.getUploads()) {
						files.add(convertingService.convertToPdf(data.getEmail(), file));
					}
				}*/
				generationService.mergeDir(data.getEmail()); // use mail as dir id to merge all previously generated docs
				//TODO: send to somewhere
				//Utils.deleteFiles(data.getEmail(), null); //delete temp files

			} catch (Exception e) {
				Utils.populateModel(data, model);
				model.put("failure", true);
				errors = new ArrayList<>();
				HashMap<String, String> error = new HashMap<>();
				error.put("error", "Bei der Generierung ist ein unerwarteter Fehler aufgetreten");
				errors.add(error);
				model.put("errorList", errors);
			}
			Utils.populateModel(new FormData(), model);
			model.put("success", true);
		}

		return "form";
	}
}
