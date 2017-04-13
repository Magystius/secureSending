package com.generali.outbound.service;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.generali.outbound.Utils;
import com.generali.outbound.exception.ConvertingException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Service Class for converting services
 * Uses a dispatcher method to generate pdf files from given input files
 * support: doc, docx, xls, xlsx, ppt, pptx, png, jpg, pdf
 * @author Tim Dekarz
 */
@Service
public class ConvertingService {

	//init local convert instance for office files; uses tmp dir for scripts
	private IConverter converter = LocalConverter.builder()
		.baseFolder(new File("./tmp"))
		.workerPool(20, 25, 2, TimeUnit.SECONDS)
		.processTimeout(5, TimeUnit.SECONDS)
		.build();

	/**
	 * Main dispatcher method. generate a pdf file from given input
	 * @param id - name if parent dir -> uses utils class
	 * @param file -> input file to convert
	 * @return -> pdf file
	 * @throws ConvertingException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public File convertToPdf(String id, MultipartFile file) throws ConvertingException, NoSuchAlgorithmException, IOException {

		String dirName = Utils.generateFile(id); //get dir name

		//valid file?
		if(file.getOriginalFilename().isEmpty()) {
			throw new ConvertingException("empty file given");
		}

		//success flag
		boolean success = false;

		//get basic data from file
		String type = file.getContentType();
		String[] temp = file.getOriginalFilename().split("\\.");
		String rawName = "";
		for(int i = 0; i < temp.length - 1; i++) {
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
		if(!success) {
			throw new ConvertingException("unknown error during convertion. check log for details");
		}

		//add to garbage collector
		Utils.addFileToGarbageCollector(dirName, psFile);

		return psFile;
	}

	/**
	 * just saves a pdf to disk
	 * @param source - input stream to persist
	 * @param target - file to write to
	 * @return - indicator if converting was successful
	 */
	private boolean savePdf(InputStream source, File target) {

		try {
			byte[] buffer = new byte[source.available()];
			source.read(buffer); //read all bytes

			OutputStream outStream = new FileOutputStream(target);
			outStream.write(buffer); //write to file
			outStream.flush();
			outStream.close();
			source.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * convert a ms office file to pdf
	 * @param officeFile - input stream of office file
	 * @param target - file to write to
	 * @param type - type of doc (doc or xls)
	 * @return - indicator if converting was successful
	 */
	private boolean convertMSOfficeToPdf(InputStream officeFile, File target, DocumentType type) {

		return converter
			.convert(officeFile).as(type)
			.to(target).as(DocumentType.PDF)
			.prioritizeWith(1000) // optional
			.execute(); //alternative you can use an async approach

	}

	/**
	 * convert an image to pdf
	 * supports png, jpg
	 * @param imageFile - byte data of image
	 * @param target - file to write to
	 * @return - indicator wether converting was successful or not
	 */
	private boolean convertImageToPdf(byte[] imageFile, File target) {

		try {
			//prepare and open new pdf
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(target));
			document.open();

			//use itext image for optimal results
			Image image = Image.getInstance(imageFile);
			image.scaleToFit(595, 842); //set to A4 format
			image.setAbsolutePosition(0, 0); //bottom-left
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
