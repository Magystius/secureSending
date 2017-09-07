package com.customerservice.outbound.service;

import com.customerservice.outbound.Utils;
import com.customerservice.outbound.exception.ConvertingException;
import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.TimeUnit;

@Service
public class ConvertingService {

	@Value("${file.output}")
	private String output;

	@Value("${upload.keepOriginalName}")
	private boolean keepOriginalName;

	private IConverter converter;

	public ConvertingService(@Value("${file.output}") String output) {
		converter = LocalConverter.builder()
			.baseFolder(new File(output))
			.workerPool(20, 25, 2, TimeUnit.SECONDS)
			.processTimeout(5, TimeUnit.SECONDS)
			.build();
	}

	public File convertToPdf(String id, MultipartFile file) {
		try {
			String dirName = Utils.generateFile(id);

			if (file.getOriginalFilename().isEmpty())
				throw new ConvertingException("empty file given");

			boolean success = false;

			String type = file.getContentType();
			String rawName = "";
			if (keepOriginalName) {
				String[] temp = file.getOriginalFilename().split("\\.");
				for (int i = 0; i < temp.length - 1; i++) {
					rawName += temp[i];
				}
			} else {
				rawName = dirName + "_upload_" + System.currentTimeMillis();
			}

			String fileName = output + dirName + "/" + rawName + ".pdf";
			File psFile = new File(fileName);
			if (!psFile.exists())
				psFile.createNewFile();

			if (type.equalsIgnoreCase("image/png") || type.equalsIgnoreCase("image/jpeg")) {
				success = convertImageToPdf(file.getBytes(), psFile);
			} else if (type.equalsIgnoreCase("application/msword") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
				success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_WORD);
			} else if (type.equalsIgnoreCase("application/vnd.ms-excel") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
				success = convertMSOfficeToPdf(file.getInputStream(), psFile, DocumentType.MS_EXCEL);
			} else if (type.equalsIgnoreCase("application/vnd.ms-powerpoint") || type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
				success = convertSlideShowToPdf(file.getInputStream(), psFile);
			} else if (type.equalsIgnoreCase("application/pdf")) {
				success = savePdf(file.getInputStream(), psFile);
			} else {
				//TODO: what should we do now?
			}

			if (!success)
				throw new ConvertingException("unknown error during convertion. check log for details");

			Utils.addFileToGarbageCollector(dirName, psFile);

			return psFile;
		} catch (Exception e) {
			throw new ConvertingException(e.getMessage());
		}
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
			.prioritizeWith(1000)
			.execute();

	}

	private boolean convertSlideShowToPdf(InputStream slideShow, File target) {
		try {
			XMLSlideShow ppt = new XMLSlideShow(slideShow);

			Document document = new Document(PageSize.A4.rotate());
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(target));
			document.open();

			Dimension pgsize = ppt.getPageSize();
			XSLFSlide[] slide = ppt.getSlides().toArray(new XSLFSlide[0]);

			for (XSLFSlide aSlide : slide) {
				BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = img.createGraphics();

				graphics.setPaint(Color.white);
				graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

				aSlide.draw(graphics);

				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ImageIO.write(img, "png", byteOut);
				Image image = Image.getInstance(byteOut.toByteArray());
				image.scaleToFit(842, 595);
				image.setAbsolutePosition(0, 0);
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
