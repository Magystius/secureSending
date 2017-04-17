package com.customerservice.outbound.service;

import com.customerservice.outbound.Utils;
import com.customerservice.outbound.exception.ConvertingException;
import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
		if(type.equalsIgnoreCase("image/png") || type.equalsIgnoreCase("image/jpeg")) {
			success = convertImageToPdf(file.getBytes(), psFile);
		} else if(type.equalsIgnoreCase("application/msword") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
			success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_WORD);
		} else if(type.equalsIgnoreCase("application/vnd.ms-excel") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
			success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_EXCEL);
		} else if(type.equalsIgnoreCase("application/vnd.ms-powerpoint") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
			success = convertSlideShowToPdf(file.getInputStream(), psFile);
		} else if(type.equalsIgnoreCase("application/pdf")) {
			success = savePdf(file.getInputStream(), psFile);
		}
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

	private boolean convertSlideShowToPdf(InputStream slideShow, File target) {

		try {
			//init slide object
			XMLSlideShow ppt = new XMLSlideShow(slideShow);

			//prepare and open new pdf
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(target));
			document.open();

			//getting the dimensions and size of the slide
			Dimension pgsize = ppt.getPageSize();
			XSLFSlide[] slide = ppt.getSlides().toArray(new XSLFSlide[0]);

			for (int i = 0; i < slide.length; i++) {
				BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = img.createGraphics();

				//clear the drawing area
				graphics.setPaint(Color.white);
				graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

				//render
				slide[i].draw(graphics);

				//convert to byte[]
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ImageIO.write(img, "png", byteOut);
				//use itext image for optimal results
				Image image = Image.getInstance(byteOut.toByteArray());
				image.scaleToFit(595, 842); //set to A4 format
				image.setAbsolutePosition(0, 0); //bottom-left
				document.add(image);
				document.newPage();
			}

			document.close();
			writer.close();
		} catch (Exception e) {
			return false;
		}

		return true;
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
