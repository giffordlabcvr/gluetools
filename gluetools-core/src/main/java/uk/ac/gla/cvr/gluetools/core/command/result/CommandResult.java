package uk.ac.gla.cvr.gluetools.core.command.result;

import javax.json.JsonObject;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class CommandResult {

	public static CommandResult OK = new OkResult();
	
	
	private Document document;
	private JsonObject jsonObject;

	public CommandResult(Document document) {
		super();
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}
	
	public JsonObject getJsonObject() {
		if(jsonObject == null) {
			jsonObject = JsonUtils.documentToJSonObjectBuilder(document).build();
		}
		return jsonObject;
	}

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
