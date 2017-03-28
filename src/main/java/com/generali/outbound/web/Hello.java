package com.generali.outbound.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.Map;

/**
 * Created by timdekarz on 27.03.17.
 */
@Controller
public class Hello {

	@Value("${message}")
	private String message;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String hello(@RequestParam(value = "name", required = false) String name, Map<String, Object> model) {

		logger.info("index route called");

		model.put("time", new Date());
		String msg = (name == null) ? message : "Hello from " + name;
		model.put("message", msg);
		model.put("title", "Hello App");

		return "form";
	}
}
