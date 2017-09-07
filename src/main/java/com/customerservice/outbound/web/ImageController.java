package com.customerservice.outbound.web;

import com.customerservice.outbound.domain.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.apache.poi.openxml4j.opc.ContentTypes.IMAGE_JPEG;
import static org.apache.poi.openxml4j.opc.ContentTypes.IMAGE_PNG;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.*;


@Controller
public class ImageController {

	private static final List<String> allowedImageTypes = Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE);
	@Value("${file.image}")
	private String image;

	@RequestMapping(value = "/image", method = RequestMethod.GET)
	public ResponseEntity<byte[]> getImage(@RequestParam(value = "id") String fileName) {
		try {
			File imageFile = new File(image + fileName);
			if (!imageFile.exists())
				return ResponseEntity.badRequest().build();

			HttpHeaders headers = prepareHttpHeaders(fileName, imageFile.length());
			return new ResponseEntity<>(Files.readAllBytes(imageFile.toPath()), headers, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
		}
	}

	@ResponseBody
	@RequestMapping(value = "/image", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<Location> saveImage(@ModelAttribute MultipartFile file) throws Exception {
		String mimeType = file.getContentType();
		if (!allowedImageTypes.contains(mimeType))
			return ResponseEntity.badRequest().build();

		String fileName = Long.toString(System.currentTimeMillis()) + "." + mimeType.substring(mimeType.length() - 3);
		File psFile = new File(image + fileName);
		if (!psFile.exists())
			psFile.createNewFile();
		new FileOutputStream(psFile).write(file.getBytes());

		return ResponseEntity.ok(Location.builder().location(fileName).build());
	}

	private HttpHeaders prepareHttpHeaders(String fileName, Long fileLength) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(valueOf(fileName.endsWith(".png") ? IMAGE_PNG : IMAGE_JPEG));
		headers.setContentLength(fileLength);
		return headers;
	}
}
