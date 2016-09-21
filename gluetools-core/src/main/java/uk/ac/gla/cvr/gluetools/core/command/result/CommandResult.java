package uk.ac.gla.cvr.gluetools.core.command.result;

import javax.json.JsonObject;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.console.ConsoleOutputFormat;
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
		Document xmlDocument = CommandDocumentXmlUtils.commandDocumentToXmlDocument(getCommandDocument());
		byte[] docBytes = GlueXmlUtils.prettyPrint(xmlDocument);
		renderCtx.output(new String(docBytes));
	}

	protected final void renderToConsoleAsJson(CommandResultRenderingContext renderCtx) {
		JsonObject jsonObject = CommandDocumentJsonUtils.commandDocumentToJsonObject(getCommandDocument());
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
