package uk.ac.gla.cvr.gluetools.core.command.result;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;

public class DocumentResult extends CommandResult {

	public DocumentResult(Document xmlDocument) {
		super(CommandDocumentXmlUtils.xmlDocumentToCommandDocument(xmlDocument));
	}

}
