package uk.ac.gla.cvr.gluetools.core.command.result;

import javax.json.JsonObject;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public abstract class CommandResult {

	public static OkResult OK = new OkResult();
	
	
	private CommandDocument commandDocument;

	protected CommandResult(String rootObjectName) {
		this.commandDocument = new CommandDocument(rootObjectName);
	}
	
	protected CommandResult(CommandDocument commandDocument) {
		this.commandDocument = commandDocument;
	}

	public CommandDocument getCommandDocument() {
		return commandDocument;
	}
	
	public final void renderResult(CommandResultRenderingContext renderCtx) {
		ResultOutputFormat consoleOutputFormat = renderCtx.getResultOutputFormat();
		switch(consoleOutputFormat) {
		case JSON:
			renderResultAsJson(renderCtx);
			break;
		case XML:
			renderResultAsXml(renderCtx);
			break;
		case TAB:
			renderResultAsTab(renderCtx);
			break;
		case CSV:
			renderResultAsCsv(renderCtx);
			break;
		default:
			if(renderCtx instanceof InteractiveCommandResultRenderingContext) {
				renderToConsoleAsText((InteractiveCommandResultRenderingContext) renderCtx);
			} else {
				renderResultAsXml(renderCtx);
			}
			break;
		}
	}

	protected final void renderResultAsXml(CommandResultRenderingContext renderCtx) {
		Document xmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(getCommandDocument());
		byte[] docBytes = GlueXmlUtils.prettyPrint(xmlDocument);
		renderCtx.output(new String(docBytes));
	}

	protected final void renderResultAsJson(CommandResultRenderingContext renderCtx) {
		JsonObject jsonObject = CommandDocumentJsonUtils.commandDocumentToJsonObject(getCommandDocument());
		renderCtx.output(JsonUtils.prettyPrint(jsonObject));
	}

	// default implementation
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		renderResultAsXml(renderCtx);
	}

	// default implementation
	protected void renderResultAsTab(CommandResultRenderingContext renderCtx) {
		renderResultAsXml(renderCtx);
	}

	// default implementation
	protected void renderResultAsCsv(CommandResultRenderingContext renderCtx) {
		renderResultAsXml(renderCtx);
	}

	
}
