package com.generali.outbound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application starter
 * @author Tim Dekarz
 */
@SpringBootApplication
public class Application {

	/**
	 * start the app
	 * @param args - cmd params
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
