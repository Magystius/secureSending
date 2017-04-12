package com.generali.outbound.service;

import com.generali.outbound.Utils;
import com.generali.outbound.domain.FormData;
import com.generali.outbound.exception.ConvertingException;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by timdekarz on 03.04.17.
 */
@Service
public class GenerationService {

	private final ConvertingService convertingService;

	@Autowired
	public GenerationService(final ConvertingService convertingService) {
		this.convertingService = convertingService;
	}
	public List<File> processAll(FormData data) throws ConvertingException {

		try {
		List<File> files = new ArrayList<>();

		//Prepare threads
		int threadNum = 1;
			if(data.getUploads().size() != 1 && !data.getUploads().get(0).getOriginalFilename().isEmpty()) {
				threadNum += data.getUploads().size();
			}
		ExecutorService executor = Executors.newFixedThreadPool(threadNum);
		List<FutureTask<File>> taskList = new ArrayList<>();

		// Start thread for custom letter
		FutureTask<File> letter = new FutureTask<>(
			() -> generateLetter(data));
		taskList.add(letter);
		executor.execute(letter);

		// Start thread if uploads available
		if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			for(MultipartFile file : data.getUploads()) {
				FutureTask<File> upFile = new FutureTask<>(
					() -> convertingService.convertToPdf(data.getEmail(), file));
				taskList.add(upFile);
				executor.execute(upFile);
			}
		}

		// Wait until all results are available and combine them at the same time
		for (int j = 0; j < threadNum; j++) {
			FutureTask<File> futureTask = taskList.get(j);
			files.add(futureTask.get());
		}
		executor.shutdown();

		return files; //return all files

		} catch (Exception e) {
			throw new ConvertingException(e.getMessage());
		}
	}

	public File generateLetter(FormData data) throws IOException, DocumentException, NoSuchAlgorithmException {

		Document document = new Document();
		//TODO: Try to use a temp file here instead
		String dirName = Utils.generateFile(data.getEmail());
		String fileName = "./tmp/" +  dirName + "/preview.pdf";
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();

		//header
		document.add(new Paragraph("Kunde:"));
		String title = (data.getTitle().equals("female")) ? "Frau" : "Herr";
		document.add(new Paragraph(title + " " + data.getFirstName() + " " + data.getLastName()));
		document.add(new Paragraph(data.getEmail()));
		Paragraph spacer = new Paragraph("Kennzeichen:");
		spacer.setSpacingBefore(12f);
		document.add(spacer);
		document.add(new Paragraph("Vorgangsnummer: " + data.getTask()));
		document.add(new Paragraph("Versicherungsnummer: " + data.getInsuranceId()));

		//rich text
		spacer = new Paragraph("Betreff: " + data.getSubject());
		spacer.setSpacingBefore(20f);
		document.add(spacer);

		InputStream messageStream = new ByteArrayInputStream(data.getMessage().getBytes(StandardCharsets.UTF_8));
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, messageStream, Charset.forName("cp1252"));

		//TODO: add images to garbage list

		//footer
		if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			spacer = new Paragraph(data.getUploads().size() +  " Anhänge");
			spacer.setSpacingBefore(20f);
			document.add(spacer);
		}

		document.close();
		writer.close();

		//add for garbage collector
		Utils.addFileToGarbageCollector(dirName, file);

		return file;
	}

	public File mergeDir(String id) throws IOException, DocumentException, NoSuchAlgorithmException {

		//prepare basic info
		String dirName = Utils.generateFile(id);
		String fileName = "./tmp/" +  dirName + "/merged.pdf";

		//open files
		Map<String, PdfReader> files = new HashMap<>();
		String[] fileNames = new File("./tmp/" + dirName).list();
		for(String name : fileNames) {
			files.put(name, new PdfReader(name));
		}

		//merge all files
		Document document = new Document();
		PdfCopy copy = new PdfCopy(document, new FileOutputStream(fileName));
		PdfCopy.PageStamp stamp;
		document.open();
		int n;
		int pageNo = 0;
		PdfImportedPage page;
		Chunk chunk;

		/*
			Iterate throw all pdf files and therefor each page of each document
			 concat them all and add a consistent page numeration
		 */
		//TODO: Check if we really need an additional page numeration
		for (Map.Entry<String, PdfReader> entry : files.entrySet()) {
			n = entry.getValue().getNumberOfPages();
			for (int i = 0; i < n; ) {
				pageNo++;
				page = copy.getImportedPage(entry.getValue(), ++i);
				stamp = copy.createPageStamp(page);
				chunk = new Chunk(String.format("Seite %d", pageNo));
				if (i == 1)
					chunk.setLocalDestination("p" + pageNo);
				ColumnText.showTextAligned(stamp.getUnderContent(),
					Element.ALIGN_RIGHT, new Phrase(chunk),
					559, 810, 0);
				stamp.alterContents();
				copy.addPage(page);
			}
		}
		//close everything
		document.close();
		for (PdfReader r : files.values()) {
			r.close();
		}
		copy.close();

		File mergedFile = new File(fileName);
		//add to garbage collector
		Utils.addFileToGarbageCollector(dirName, mergedFile);

		return mergedFile;
	}
}
