/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
