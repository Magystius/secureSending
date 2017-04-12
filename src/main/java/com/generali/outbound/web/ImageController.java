package com.generali.outbound.web;

import com.generali.outbound.domain.Location;
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
import java.nio.file.Files;

/**
 * Created by Tim on 12.04.2017.
 */
@Controller
public class ImageController {

	@RequestMapping(value = "/image", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getImage(@RequestParam(value = "id") String fileName) {

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

	@RequestMapping(value = "/image", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody
	Location saveImage(@ModelAttribute MultipartFile file, HttpServletResponse response) throws Exception {

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
}
