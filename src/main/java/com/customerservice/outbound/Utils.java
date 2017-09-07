package com.customerservice.outbound;

import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.exception.DeletionException;
import org.springframework.beans.factory.annotation.Value;

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

public class Utils {

	@Value("${file.output}")
	private static String output;

	private static Map<String, List<File>> generatedFiles = new HashMap<>();

	public static void addFileToGarbageCollector(String dir, File file) {
		if (dir != null && !dir.isEmpty() && file != null)
			generatedFiles.get(dir).add(file);
	}

	public static void populateModel(FormData data, Map<String, Object> model) {
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
	}

	public static String generateFile(String email) throws RuntimeException {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(email.getBytes(), 0, email.length());
			String fileName = new BigInteger(1, md5.digest()).toString(16);
			File file = new File(output + fileName);
			if (!file.exists())
				file.mkdirs();

			if (!generatedFiles.containsKey(fileName))
				generatedFiles.put(fileName, new ArrayList<>());

			return fileName;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static void deleteFiles(String id, List<File> optFiles) throws NoSuchAlgorithmException, IOException, DeletionException {
		String dirName = generateFile(id);

		if (generatedFiles.containsKey(dirName)) {
			List<File> files = generatedFiles.get(dirName);
			files.forEach(File::delete);
			File dir = new File(output + dirName);
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir.toPath());
			if (!dirStream.iterator().hasNext())
				throw new DeletionException("parent not empty. error during deletion");
			dir.delete();
			generatedFiles.remove(dirName);
		}

		if (optFiles != null && optFiles.size() > 0) {
			optFiles.forEach(File::delete);
		}
	}
}
