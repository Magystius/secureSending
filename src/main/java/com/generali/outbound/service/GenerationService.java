package com.generali.outbound.service;

import com.generali.outbound.domain.FormData;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by timdekarz on 03.04.17.
 */
@Service
public class GenerationService {

	public File generateLetter(FormData data) throws IOException, DocumentException {


		Document document = new Document();
		//TODO: Try to use a temp file here instead 
		String fileName = "./tmp/" + Long.toString(System.currentTimeMillis()) + ".pdf";
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();

		//test
		document.add(new Paragraph(data.getFirstName() + " " + data.getLastName()));
		document.add(new Paragraph(data.getEmail()));

		//rich text
		PdfPTable table = new PdfPTable(2);
		table.addCell("Some rich text:");
		PdfPCell cell = new PdfPCell();
		for (Element e : XMLWorkerHelper.parseToElementList(data.getMessage(), null)) {
			cell.addElement(e);
		}
		table.addCell(cell);
		document.add(table);
		document.close();

		return file;
	}

	public List<File> convertToPDF(List<MultipartFile> uploads) {

		return new ArrayList<>();
	}


}
