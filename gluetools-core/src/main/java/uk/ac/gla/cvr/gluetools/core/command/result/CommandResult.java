package uk.ac.gla.cvr.gluetools.core.command.result;

import javax.json.JsonObject;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.console.ConsoleOutputFormat;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public abstract class CommandResult {

	public static OkResult OK = new OkResult();
	
	
	private DocumentBuilder documentBuilder;
	private DocumentReader documentReader;

	private Document xmlDocument;
	private JsonObject jsonObject;

	protected CommandResult(String rootObjectName) {
		this.documentBuilder = new DocumentBuilder(rootObjectName);
	}
	
	protected DocumentBuilder getDocumentBuilder() {
		return documentBuilder;
	}
	
	protected DocumentReader getDocumentReader() {
		if(documentReader == null) {
			documentReader = new DocumentReader(getDocument());
		}
		return documentReader;
	}

	public Document getDocument() {
		if(xmlDocument == null) {
			xmlDocument = documentBuilder.getXmlDocument();
		}
		return xmlDocument;
	}
	
	public JsonObject getJsonObject() {
		if(jsonObject == null) {
			jsonObject = documentBuilder.getJsonObject();
		}
		return jsonObject;
	}

	public final void renderToConsole(CommandResultRenderingContext renderCtx) {
		ConsoleOutputFormat consoleOutputFormat = renderCtx.getConsoleOutputFormat();
		switch(consoleOutputFormat) {
		case JSON:
			renderToConsoleAsJson(renderCtx);
			break;
		case XML:
			renderToConsoleAsXml(renderCtx);
			break;
		case TAB:
			renderToConsoleAsTab(renderCtx);
			break;
		case CSV:
			renderToConsoleAsCsv(renderCtx);
			break;
		default:
			renderToConsoleAsText(renderCtx);
			break;
		}
	}

	protected final void renderToConsoleAsXml(CommandResultRenderingContext renderCtx) {
		Document document = getDocument();
		byte[] docBytes = GlueXmlUtils.prettyPrint(document);
		renderCtx.output(new String(docBytes));
	}

	protected final void renderToConsoleAsJson(CommandResultRenderingContext renderCtx) {
		JsonObject jsonObject = getJsonObject();
		renderCtx.output(JsonUtils.prettyPrint(jsonObject));
	}

	// default implementation
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderToConsoleAsXml(renderCtx);
	}

	// default implementation
	protected void renderToConsoleAsTab(CommandResultRenderingContext renderCtx) {
		renderToConsoleAsText(renderCtx);
	}

	// default implementation
	protected void renderToConsoleAsCsv(CommandResultRenderingContext renderCtx) {
		renderToConsoleAsText(renderCtx);
	}

	
}
