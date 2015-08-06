package uk.ac.gla.cvr.gluetools.core.command.result;

import javax.json.JsonObject;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public abstract class CommandResult {

	public static OkResult OK = new OkResult();
	
	
	private DocumentBuilder documentBuilder;

	private Document xmlDocument;
	private JsonObject jsonObject;

	protected CommandResult(String rootObjectName) {
		this.documentBuilder = new DocumentBuilder(rootObjectName);
	}
	
	protected DocumentBuilder getDocumentBuilder() {
		return documentBuilder;
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
		if(renderCtx.getConsoleOutputFormat().equals("json")) {
			renderToConsoleAsJson(renderCtx);
		} else if(renderCtx.getConsoleOutputFormat().equals("xml")) {
			renderToConsoleAsXml(renderCtx);
		} else {
			renderToConsoleAsText(renderCtx);
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

	
}
