package com.customerservice.outbound;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by timdekarz on 29.03.17.
 */
@ControllerAdvice
public class ExceptionHandler extends ResponseEntityExceptionHandler {

	@org.springframework.web.bind.annotation.ExceptionHandler(MultipartException.class)
	ModelAndView handleFileException(HttpServletRequest request, Exception exception) {

		//get parameters
		//TODO: This doest not work, cause request has no params!
		Map<String, Object> model = new HashMap<>();
		Map<String, String[]> data = request.getParameterMap();

		Boolean titleUnselected, titleFemale, titleMale;
		titleUnselected = !data.get("title")[0].equals("female") && !data.get("title")[0].equals("male");
		titleFemale = data.get("title")[0].equals("female");
		titleMale = data.get("title")[0].equals("male");
		model.put("titleUnselected", titleUnselected);
		model.put("titleFemale", titleFemale);
		model.put("titleMale", titleMale);

		model.put("firstName", data.get("firstName")[0]);
		model.put("lastName", data.get("lastName")[0]);

		model.put("email", data.get("email")[0]);
		model.put("tel", data.get("telephone")[0]);
		boolean telSelect = !(data.get("telephone")[0] == null || data.get("telephone")[0].isEmpty());
		model.put("telSelect", telSelect);

		model.put("taskId", data.get("taskId")[0]);
		model.put("contractId", data.get("contractId")[0]);

		model.put("success", false);
		model.put("failure", false);

		List<Map<String, String>> errors = new ArrayList<>();
		HashMap<String, String> error = new HashMap<>();
		error.put("error", exception.getMessage());
		errors.add(error);
		model.put("errorList", errors);

		return new ModelAndView("/form", model);
	}
}
