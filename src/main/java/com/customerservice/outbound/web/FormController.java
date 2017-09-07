package com.customerservice.outbound.web;

import com.customerservice.outbound.Utils;
import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.service.ConvertingService;
import com.customerservice.outbound.service.GenerationService;
import com.customerservice.outbound.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	@RequestMapping(value = "/form", method = RequestMethod.GET)
	public String initial(FormData data,
						  Map<String, Object> model) {
		Utils.populateModel(data, model);
		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String submit(@ModelAttribute FormData data, Map<String, Object> model) {
		//TODO: delete this
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (!validationService.handleValidation(data, model))
			return "form";

		try {
			List<File> files = new ArrayList<>();
			if (useParallelThreads)
				files.addAll(generationService.processAll(data));
			else {
				files.add(generationService.generateLetter(data, letterName));
				if (!data.getUploads().get(0).getOriginalFilename().isEmpty())
					data.getUploads().forEach(file -> files.add(convertingService.convertToPdf(data.getEmail(), file)));
			}
			if (merge)
				generationService.mergeDir(data.getEmail(), mergeName);

			//TODO: send to somewhere
			if (deleteFiles)
				Utils.deleteFiles(data.getEmail(), null);
		} catch (Exception e) {
			prepareErrorModel(data, model);
			return "form";
		}

		Utils.populateModel(new FormData(), model);
		model.put("success", true);

		return "form";
	}

	private void prepareErrorModel(@ModelAttribute FormData data, Map<String, Object> model) {
		Utils.populateModel(data, model);
		model.put("failure", true);
		List<Map<String, String>> errors = new ArrayList<>();
		HashMap<String, String> error = new HashMap<>();
		error.put("error", "Bei der Generierung ist ein unerwarteter Fehler aufgetreten");
		errors.add(error);
		model.put("errorList", errors);
	}
}
