package com.generali.outbound;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		//SpringApplication.run(Application.class, args);

		File wordFile = new File("./tmp/TEST.docx"),
			target = new File("./tmp/TEST.pdf");
		IConverter converter = LocalConverter.builder()
			.baseFolder(new File("./tmp"))
		    .workerPool(20, 25, 2, TimeUnit.SECONDS)
			.processTimeout(5, TimeUnit.SECONDS)
			.build();
		Future<Boolean> conversion = converter
			.convert(wordFile).as(DocumentType.MS_WORD)
			.to(target).as(DocumentType.PDF)
			.prioritizeWith(1000) // optional
			.schedule();

	}
}
