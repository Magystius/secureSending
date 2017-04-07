package com.generali.outbound.service;

import com.generali.outbound.domain.FormData;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by timdekarz on 03.04.17.
 */
@Service
public class GenerationService {

	public File generateLetter(FormData data) throws IOException, DocumentException, NoSuchAlgorithmException {

		Document document = new Document();
		//TODO: Try to use a temp file here instead
		String fileName = "./tmp/" + generateFile(data.getEmail()) + "/preview.pdf";
		//TODO: This produces a dir not a file -> FIX
		File file = new File(fileName);
		if (!file.exists()) {
			file.mkdirs();
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

		//footer
		if(data.getUploads().size() != 1 && !data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			spacer = new Paragraph(data.getUploads().size() +  " Anhänge");
			spacer.setSpacingBefore(20f);
			document.add(spacer);
		}

		document.close();
		writer.close();

		return file;
	}

	public List<File> convertToPDF(List<MultipartFile> uploads) {

		return new ArrayList<>();
	}

	private String generateFile(String email) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(email.getBytes(),0,email.length());
		String fileName = new BigInteger(1,md5.digest()).toString(16);
		File file = new File("./tmp/" + fileName);
		if (!file.exists()) {
			file.mkdirs();
		}
		return fileName;
	}

}
