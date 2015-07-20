package uk.ac.gla.cvr.gluetools.core.command.result;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class DocumentResult extends CommandResult {

	private Document document;

	public DocumentResult(Document document) {
		super();
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	@Override
	public final void renderToConsole(CommandResultRenderingContext renderCtx) {
		if(renderCtx.getConsoleOutputFormat().equals("xml")) {
			renderToConsoleAsXml(renderCtx);
		} else {
			renderToConsoleAsText(renderCtx);
		}
	}

	protected final void renderToConsoleAsXml(CommandResultRenderingContext renderCtx) {
		Document document = getDocument();
		byte[] docBytes = XmlUtils.prettyPrint(document);
		renderCtx.output(new String(docBytes));
	}

	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderToConsoleAsXml(renderCtx);
	}
	
}
