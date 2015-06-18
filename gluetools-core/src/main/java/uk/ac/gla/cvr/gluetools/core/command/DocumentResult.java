package uk.ac.gla.cvr.gluetools.core.command;

import org.w3c.dom.Document;

public class DocumentResult extends CommandResult {

	private Document document;

	public DocumentResult(Document document) {
		super();
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}
	
}
