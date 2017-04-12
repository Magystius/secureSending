package com.generali.outbound.service;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.generali.outbound.Utils;
import com.generali.outbound.exception.ConvertingException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Tim on 12.04.2017.
 */
@Service
public class ConvertingService {

	private IConverter converter = null;
		/*LocalConverter.builder()
		.baseFolder(new File("./tmp"))
		.workerPool(20, 25, 2, TimeUnit.SECONDS)
		.processTimeout(5, TimeUnit.SECONDS)
		.build();*/

	public File convertToPdf(String id, MultipartFile file) throws ConvertingException, NoSuchAlgorithmException, IOException {

		String dirName = Utils.generateFile(id);

		//valid file?
		if(file.getOriginalFilename().isEmpty()) {
			throw new ConvertingException("empty file given");
		}

		//success flag
		boolean success = false;

		//get basic data from file
		String type = file.getContentType();
		String[] temp = file.getOriginalFilename().split(".");
		String rawName = "";
		for(int i = 0; i < temp.length; i++) {
			rawName += temp[i];
		}

		//generate file
		String fileName = "./tmp/" + dirName + "/" + rawName + ".pdf";
		File psFile = new File(fileName);
		if (!psFile.exists()) {
			psFile.createNewFile();
		}

		//dispatching file content type
		if(type.contains("png") || type.contains("jpg") || type.contains("jpeg")) {
			success = convertImageToPdf(file.getBytes(), psFile);
		} else if(type.contains("doc") || type.contains("docx")) {
			success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_WORD);
		} else if(type.contains("xls") || type.contains("xlsx")) {
			success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_EXCEL);
		} else if(type.contains("pdf")) {
			success = savePdf(file.getInputStream(), psFile);
		}
		//TODO: add here support for powerpoint
		else {
			//unsupported type
			//TODO: what should we do now?
		}

		//check success generation
		if(success) {
			throw new ConvertingException("unknown error during convertion. check log for details");
		}

		//add to garbage collector
		Utils.addFileToGarbageCollector(dirName, psFile);

		return psFile;
	}

	private boolean savePdf(InputStream source, File target) {

		try {
			byte[] buffer = new byte[source.available()];
			source.read(buffer);

			OutputStream outStream = new FileOutputStream(target);
			outStream.write(buffer);
			outStream.flush();
			outStream.close();
			source.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	private boolean convertMSOfficeToPdf(InputStream officeFile, File target, DocumentType type) {

		return converter
			.convert(officeFile).as(type)
			.to(target).as(DocumentType.PDF)
			.prioritizeWith(1000) // optional
			.execute();

	}

	private boolean convertImageToPdf(byte[] imageFile, File target) {

		try {
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(target));
			document.open();

			Image image = Image.getInstance(imageFile);
			image.scaleToFit(595, 842);
			image.setAbsolutePosition(0, 0);
			document.add(image);
			document.newPage();

			document.close();
			writer.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}
}
