package com.generali.outbound.service;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.generali.outbound.domain.FormData;
import com.generali.outbound.exception.ConvertingException;
import com.generali.outbound.exception.DeletionException;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.tool.xml.Pipeline;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.AbstractImageProvider;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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

	private IConverter converter = null;
		/*LocalConverter.builder()
		.baseFolder(new File("./tmp"))
		.workerPool(20, 25, 2, TimeUnit.SECONDS)
		.processTimeout(5, TimeUnit.SECONDS)
		.build();*/

	private static Map<String, List<File>> generatedFiles = new HashMap<>();


	public List<File> generateAll(FormData data) throws ConvertingException {

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
					() -> convertToPdf(data.getEmail(), file));
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
		String dirName = generateFile(data.getEmail());
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

		HtmlPipelineContext htmlContext = new HtmlPipelineContext(null);
		htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
		htmlContext.setImageProvider(new AbstractImageProvider() {

			public String getImageRootPath() {

				return "http://localhost:8080";

			}

		});
		CSSResolver cssResolver =
			XMLWorkerHelper.getInstance().getDefaultCssResolver(true);
		Pipeline<?> pipeline =
			new CssResolverPipeline(cssResolver,
				new HtmlPipeline(htmlContext,
					new PdfWriterPipeline(document, writer)));
		XMLWorker worker = new XMLWorker(pipeline, true);
		XMLParser p = new XMLParser(worker);

		p.parse(new ByteArrayInputStream(data.getMessage().getBytes(StandardCharsets.UTF_8)));
		/*for (Element e : XMLWorkerHelper.parseToElementList(data.getMessage(), null)) {
			document.add(e);
		}*/

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
		generatedFiles.get(dirName).add(file);

		return file;
	}

	public File convertToPdf(String id, MultipartFile file) throws ConvertingException, NoSuchAlgorithmException, IOException {

		String dirName = generateFile(id);

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
		generatedFiles.get(dirName).add(psFile);

		return psFile;
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

	public File mergeDir(String id) throws IOException, DocumentException, NoSuchAlgorithmException {

		//prepare basic info
		String dirName = generateFile(id);
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
		generatedFiles.get(dirName).add(mergedFile);

		return mergedFile;
	}

	public void deleteFiles(String id, List<File> optFiles) throws NoSuchAlgorithmException, IOException, DeletionException {
			String dirName = generateFile(id);
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

	private String generateFile(String email) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(email.getBytes(),0,email.length());
		String fileName = new BigInteger(1,md5.digest()).toString(16);
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

}
