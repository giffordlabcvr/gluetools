package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Document;

public class DocumentReader extends ObjectReader {

	public DocumentReader(Document document) {
		super(document.getDocumentElement());
	}
	
	
}
