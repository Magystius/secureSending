package com.customerservice.outbound.service;

import com.customerservice.outbound.Utils;
import com.customerservice.outbound.domain.FormData;
import com.customerservice.outbound.exception.ConvertingException;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service
public class GenerationService {

	@Value("${file.output}")
	private String output;

	@Value("${file.image}")
	private String image;

	@Value("${merge.pageNumbers}")
	private boolean pageNumbers;

	@Value("${process.letterName}")
	private String letterName;

	private final ConvertingService convertingService;

	@Autowired
	public GenerationService(final ConvertingService convertingService) {
		this.convertingService = convertingService;
	}

	public List<File> processAll(final FormData data) throws ConvertingException {
		try {
			int threadNum = 1;
			if (data.getUploads().size() != 1 && !data.getUploads().get(0).getOriginalFilename().isEmpty())
				threadNum += data.getUploads().size();
			ExecutorService executor = Executors.newFixedThreadPool(threadNum);
			List<FutureTask<File>> taskList = new ArrayList<>();

			FutureTask<File> letter = new FutureTask<>(() -> generateLetter(data, letterName));
			taskList.add(letter);
			executor.execute(letter);

			if (!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
				data.getUploads().stream()
					.map(file -> new FutureTask<>(() -> convertingService.convertToPdf(data.getEmail(), file)))
					.forEach(upFile -> {
						taskList.add(upFile);
						executor.execute(upFile);
					});
			}

			final List<File> files = taskList.stream().map(futureTask -> {
				try {
					return futureTask.get();
				} catch (Exception e) {
					return null;
				}
			}).collect(Collectors.toList());
			executor.shutdown();

			return files;

		} catch (Exception e) {
			throw new ConvertingException(e.getMessage());
		}
	}

	public File generateLetter(FormData data, String fileName) throws Exception {
		Document document = new Document();
		String dirName = Utils.generateFile(data.getEmail());
		String path = output + dirName + "/" + fileName + ".pdf";
		File file = new File(path);
		if (!file.exists())
			file.createNewFile();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();

		document.add(new Paragraph("Kunde:"));
		String title = (data.getTitle().equals("female")) ? "Frau" : "Herr";
		document.add(new Paragraph(title + " " + data.getFirstName() + " " + data.getLastName()));
		document.add(new Paragraph(data.getEmail()));
		Paragraph spacer = new Paragraph("Kennzeichen:");
		spacer.setSpacingBefore(12f);
		document.add(spacer);
		document.add(new Paragraph("Vorgangsnummer: " + data.getTaskId()));
		document.add(new Paragraph("Vertragsnummer: " + data.getContractId()));

		spacer = new Paragraph("Betreff: " + data.getSubject());
		spacer.setSpacingBefore(20f);
		document.add(spacer);

		data.setMessage(data.getMessage().replace("image?id=", image));
		InputStream messageStream = new ByteArrayInputStream(data.getMessage().getBytes(StandardCharsets.UTF_8));
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, messageStream, Charset.forName("cp1252"));

		//TODO: add images to garbage list

		if (!data.getUploads().get(0).getOriginalFilename().isEmpty()) {
			spacer = new Paragraph(data.getUploads().size() + " Anhänge");
			spacer.setSpacingBefore(20f);
			document.add(spacer);
		}

		document.close();
		writer.close();

		Utils.addFileToGarbageCollector(dirName, file);

		return file;
	}

	public void mergeDir(String id, String fileName) throws Exception {
		String dirName = Utils.generateFile(id);
		String path = output + dirName + "/" + fileName + ".pdf";

		String[] fileNames = new File(output + dirName).list();
		Map<String, PdfReader> files = new HashMap<>();
		if (fileNames != null) {
			files = Arrays.stream(fileNames).collect(Collectors.toMap(identity(), name -> {
				try {
					return new PdfReader(output + dirName + "/" + fileName);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}));
		}

		Document document = new Document();
		PdfCopy copy = new PdfCopy(document, new FileOutputStream(path));
		PdfCopy.PageStamp stamp;
		document.open();
		int n;
		int pageNo = 0;
		PdfImportedPage page;
		Chunk chunk;

		//TODO: Check if we really need an additional page numeration
		for (Map.Entry<String, PdfReader> entry : files.entrySet()) {
			n = entry.getValue().getNumberOfPages();
			for (int i = 0; i < n; ) {
				pageNo++;
				page = copy.getImportedPage(entry.getValue(), ++i);
				if (pageNumbers) {
					stamp = copy.createPageStamp(page);
					chunk = new Chunk(String.format("Seite %d", pageNo));
					if (i == 1)
						chunk.setLocalDestination("p" + pageNo);
					ColumnText.showTextAligned(stamp.getUnderContent(),
						Element.ALIGN_RIGHT, new Phrase(chunk),
						559, 810, 0);
					stamp.alterContents();
				}
				copy.addPage(page);
			}
		}

		document.close();
		for (PdfReader r : files.values())
			r.close();
		copy.close();

		Utils.addFileToGarbageCollector(dirName, new File(path));
	}
}
