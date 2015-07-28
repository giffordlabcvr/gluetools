package uk.ac.gla.cvr.gluetools.core.command.console;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class ConsoleCommandResult extends CommandResult {

	public ConsoleCommandResult(String text) {
		super(cmdDocFromText(text));
	}

	private static Document cmdDocFromText(String text) {
		Document doc = XmlUtils.newDocument();
		Element rootElem = doc.createElement("consoleCommandResult");
		doc.appendChild(rootElem);
		rootElem.appendChild(doc.createTextNode(text));
		return doc;
	}
	
	@Override
	protected final void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output(XmlUtils.getXPathString(getDocument(), "/consoleCommandResult/text()"));
	}
	
	
	
}
