import com.itextpdf.text.*;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by timdekarz on 12.04.17.
 */
public class TETS {

	public static void main(String[] args) throws IOException, DocumentException {
		//prepare basic info
		String dirName = "a65dadf2e0bcf32e89d689ac51e3815f";
		String fileName = "./tmp/" +  dirName + "/merged.pdf";

		//open files
		Map<String, PdfReader> files = new HashMap<>();
		String[] fileNames = new File("./tmp/" + dirName).list();
		for(String name : fileNames) {
			files.put(name, new PdfReader("./tmp/" + dirName + "/" + name));
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
		for (Map.Entry<String, PdfReader> entry : files.entrySet()) {
			n = entry.getValue().getNumberOfPages();
			for (int i = 0; i < n; ) {
				pageNo++;
				page = copy.getImportedPage(entry.getValue(), ++i);
				stamp = copy.createPageStamp(page);
				chunk = new Chunk(String.format("Page %d", pageNo));
				if (i == 1)
					chunk.setLocalDestination("p" + pageNo);
				ColumnText.showTextAligned(stamp.getUnderContent(),
					Element.ALIGN_RIGHT, new Phrase(chunk),
					559, 810, 0);
				stamp.alterContents();
				copy.addPage(page);
			}
		}
		document.close();
		for (PdfReader r : files.values()) {
			r.close();
		}
		copy.close();
	}
}
