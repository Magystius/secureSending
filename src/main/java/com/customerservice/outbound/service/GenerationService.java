package com.customerservice.outbound.service;

import com.customerservice.outbound.Utils;
import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.exception.ConvertingException;
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
 * Service for working with pdfs
 * generates a custom letter from formdata
 * @author Tim Dekarz
 */
@Service
public class GenerationService {

	private final ConvertingService convertingService;

	@Autowired
	public GenerationService(final ConvertingService convertingService) {
		this.convertingService = convertingService;
	}

	/**
	 * Convience method to enable parallel converting
	 * open threads for letter and uploads, runs them parallel and collect output
	 * @param data - data to process
	 * @return - list of generated files
	 * @throws ConvertingException
	 */
	public List<File> processAll(final FormData data) throws ConvertingException {

		try {
		List<File> files = new ArrayList<>();

		//Prepare threads
		int threadNum = 1;
			//if uploads present
			if(data.getUploads().size() != 1 && !data.getUploads().get(0).getOriginalFilename().isEmpty()) {
				threadNum += data.getUploads().size();
			}
		ExecutorService executor = Executors.newFixedThreadPool(threadNum);
		List<FutureTask<File>> taskList = new ArrayList<>();

		// Start thread for custom letter
		FutureTask<File> letter = new FutureTask<>(new Callable<File>() {
				@Override
				public File call() {
					try {
						return generateLetter(data);
					} catch (Exception e) {
						return null;
					}
				}
			});
		taskList.add(letter);
		executor.execute(letter);

		// Start thread if uploads available
		if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			for(final MultipartFile file : data.getUploads()) {
				FutureTask<File> upFile = new FutureTask<>(new Callable<File>() {
						@Override
						public File call() {
							try {
								return convertingService.convertToPdf(data.getEmail(), file);
							} catch (Exception e) {
								return null;
							}
						}
					});
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

	/**
	 * generated a custom letter in pdf format from form input
	 * @param data - data to process
	 * @return - generated file
	 * @throws IOException
	 * @throws DocumentException
	 * @throws NoSuchAlgorithmException
	 */
	public File generateLetter(FormData data) throws IOException, DocumentException, NoSuchAlgorithmException {

		//prepare doc and file to write to
		Document document = new Document();
		String dirName = Utils.generateFile(data.getEmail());
		String fileName = "./tmp/" +  dirName + "/preview.pdf";
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();

		//add meta data from form as header
		document.add(new Paragraph("Kunde:"));
		String title = (data.getTitle().equals("female")) ? "Frau" : "Herr";
		document.add(new Paragraph(title + " " + data.getFirstName() + " " + data.getLastName()));
		document.add(new Paragraph(data.getEmail()));
		Paragraph spacer = new Paragraph("Kennzeichen:");
		spacer.setSpacingBefore(12f);
		document.add(spacer);
		document.add(new Paragraph("Vorgangsnummer: " + data.getTaskId()));
		document.add(new Paragraph("Versicherungsnummer: " + data.getContractId()));

		//add subject and messge from rtf editor
		spacer = new Paragraph("Betreff: " + data.getSubject());
		spacer.setSpacingBefore(20f);
		document.add(spacer);

		data.setMessage(data.getMessage().replace("image?id=", "./img/")); //replace dynamic request url with local file path
		//process with helper class from lib
		InputStream messageStream = new ByteArrayInputStream(data.getMessage().getBytes(StandardCharsets.UTF_8));
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, messageStream, Charset.forName("cp1252"));

		//TODO: add images to garbage list

		//add footer if uploads present
		if(!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			spacer = new Paragraph(data.getUploads().size() +  " Anhänge");
			spacer.setSpacingBefore(20f);
			document.add(spacer);
		}

		//finishing stuff
		document.close();
		writer.close();

		//add for garbage collector
		Utils.addFileToGarbageCollector(dirName, file);

		return file;
	}

	/**
	 * merges all files in a given dir to one pdf doc
	 * This only works properly if all files are already pdfs!
	 * @param id - dir name -> uses utils clsss
	 * @return - generated merged file
	 * @throws IOException
	 * @throws DocumentException
	 * @throws NoSuchAlgorithmException
	 */
	public File mergeDir(String id) throws IOException, DocumentException, NoSuchAlgorithmException {

		//prepare basic info
		String dirName = Utils.generateFile(id);
		String fileName = "./tmp/" +  dirName + "/merged.pdf";

		//open files
		Map<String, PdfReader> files = new HashMap<>();
		String[] fileNames = new File("./tmp/" + dirName).list();
		for(String name : fileNames) {
			//TODO: should we provide a check for pdfs here?
			files.put(name, new PdfReader("./tmp/" + dirName + "/" + name)); //if we have a non pdf file present -> exception
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
