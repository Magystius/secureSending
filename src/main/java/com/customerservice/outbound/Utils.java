package com.customerservice.outbound;

import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.exception.DeletionException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util Class with misc methods
 * TODO: probably better class design possible
 * @author Tim Dekarz
 */
public class Utils {

	private static Map<String, List<File>> generatedFiles = new HashMap<>(); //list for garbage collector

	/**
	 * add a file to garbage collector list
	 * @param dir - parent dir as key
	 * @param file - file object for deletetion
	 */
	public static void addFileToGarbageCollector(String dir, File file) {
		//check if valid file
		if(dir != null && !dir.isEmpty() && file != null) {
			generatedFiles.get(dir).add(file);
		}
	}

	/**
	 * prepares map with form data for web form
	 * @param data - form data
	 * @param model - map to populate
	 * @return - filled map
	 */
	public static Map<String, Object> populateModel(FormData data, Map<String, Object> model) {

		//title choose
		Boolean titleUnselected, titleFemale, titleMale;
		titleUnselected = !data.getTitle().equals("female") && !data.getTitle().equals("male");
		titleFemale = data.getTitle().equals("female");
		titleMale = data.getTitle().equals("male");
		model.put("titleUnselected", titleUnselected);
		model.put("titleFemale", titleFemale);
		model.put("titleMale", titleMale);

		model.put("firstName", data.getFirstName());
		model.put("lastName", data.getLastName());

		model.put("email", data.getEmail());
		model.put("password", data.getPassword());

		model.put("taskId", data.getTaskId());
		model.put("contractId", data.getContractId());

		model.put("subject", data.getSubject());
		model.put("message", data.getMessage());

		model.put("success", false);
		model.put("failure", false);

		return model;
	}

	/**
	 * generate dir for files via hash
	 * @param email - id as hash base
	 * @return - string of dirName
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateFile(String email) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(email.getBytes(),0,email.length());
		String fileName = new BigInteger(1,md5.digest()).toString(16); //hash fileName
		//create parent dir
		File file = new File("./tmp/" + fileName);
		if (!file.exists()) {
			file.mkdirs();
		}

		//init list for garbage collector
		if(!generatedFiles.containsKey(fileName)) {
			generatedFiles.put(fileName, new ArrayList<>());
		}

		return fileName;
	}

	/**
	 * delete method for old files
	 * @param id - parent dir -> uses utils class
	 * @param optFiles - optional files to delete
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws DeletionException
	 */
	public static void deleteFiles(String id, List<File> optFiles) throws NoSuchAlgorithmException, IOException, DeletionException {
		String dirName = generateFile(id); //get dirName
		//check garbage collector
		if(generatedFiles.containsKey(dirName)) {
			//delete each file
			List<File> files = generatedFiles.get(dirName);
			for(File file : files) {
				file.delete();
			}
			//delete parent dir
			File dir = new File("./tmp/" + dirName);
			//check if empty
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir.toPath());
			if(!dirStream.iterator().hasNext()) {
				throw new DeletionException("parent not empty. error during deletion");
			}
			dir.delete();
			//delete entry in garbage list
			generatedFiles.remove(dirName);
		}
		//extra files found
		if(optFiles != null && optFiles.size() > 0) {
			for(File file : optFiles) {
				file.delete();
			}
		}
	}
}
