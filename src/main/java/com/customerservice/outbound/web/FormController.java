package com.customerservice.outbound.web;

import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.service.GenerationService;
import com.customerservice.outbound.service.ValidationService;
import com.customerservice.outbound.Utils;
import com.customerservice.outbound.service.ConvertingService;
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

	@Value("${process.useParallelThreads}")
	boolean useParallelThreads;

	@Value("${process.deleteFiles}")
	boolean deleteFiles;

	@Value("${merge}")
	boolean merge;

	@Value("${customer}")
	private String customer;

	@Value("${dev.delay}")
	private int delay;

	@Value("${process.letterName}")
	private String letterName;

	@Value("${process.mergeName}")
	private String mergeName;

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

		//use delay for dev urposes
		if(delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//validation
		if(!validationService.handleValidation(data, model)) {
			return "form";
		}

		//process input
		try {
			List<File> files = new ArrayList<>();
			if(useParallelThreads) {
				files.addAll(generationService.processAll(data)); //use this method for parallel converting/generating
			} else {
				files.add(generationService.generateLetter(data, letterName));
				if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
					for(MultipartFile file : data.getUploads()) {
						files.add(convertingService.convertToPdf(data.getEmail(), file));
					}
				}
			}
			if(merge) {
				generationService.mergeDir(data.getEmail(), mergeName); // use mail as dir id to merge all previously generated docs
			}
			//TODO: send to somewhere
			if(deleteFiles) {
				Utils.deleteFiles(data.getEmail(), null); //delete temp files
			}

		} catch (Exception e) {
			Utils.populateModel(data, model);
			model.put("failure", true);
			List<Map<String, String>> errors = new ArrayList<>();
			HashMap<String, String> error = new HashMap<>();
			error.put("error", "Bei der Generierung ist ein unerwarteter Fehler aufgetreten");
			errors.add(error);
			model.put("errorList", errors);

			return "form";
		}
		Utils.populateModel(new FormData(), model);
		model.put("success", true);

		return "form";
	}
}
