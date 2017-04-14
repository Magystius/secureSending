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
 * Web Controller for handling image uploads
 * @author Tim Dekarz
 */
@Controller
public class ImageController {

	/**
	 * recieve an image via id
	 * @param fileName - image id
	 * @return - response with image data
	 */
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

			return new ResponseEntity<>(Files.readAllBytes(image.toPath()), headers, HttpStatus.OK); //read file and return it in response
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * upload and image and return location
	 * @param file - image data
	 * @param response - response object
	 * @return - location object with image url
	 * @throws Exception
	 */
	@RequestMapping(value = "/image", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody Location saveImage(@ModelAttribute MultipartFile file, HttpServletResponse response) throws Exception {

		//detect type of file
		String type;
		if(file.getContentType().contains("png")) {
			type = "png";
		} else if(file.getContentType().contains("jpg") || file.getContentType().contains("jpeg")) {
			type = "jpg";
		} else {
			response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
			return new Location();
		}
		//generate unique name and file
		String fileName = Long.toString(System.currentTimeMillis()) + "." + type;
		File psFile = new File("./img/" + fileName);
		if (!psFile.exists()) {
			psFile.createNewFile();
		}
		//persist file
		new FileOutputStream(psFile).write(file.getBytes());

		//init location object with path
		Location loc = new Location();
		loc.setLocation(fileName);

		return loc;
	}
}
