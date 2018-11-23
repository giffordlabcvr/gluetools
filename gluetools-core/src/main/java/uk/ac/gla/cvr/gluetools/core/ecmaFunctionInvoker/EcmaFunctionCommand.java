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
package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class EcmaFunctionCommand extends ModulePluginCommand<CommandResult, EcmaFunctionInvoker> implements ProvidedProjectModeCommand {

	public static final String FUNCTION_NAME = "functionName";
	public static final String ARGUMENT = "argument";
	public static final String DOCUMENT = "document";
	
	
	private String functionName;
	private List<String> arguments;
	private CommandDocument document;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.functionName = PluginUtils.configureStringProperty(configElem, FUNCTION_NAME, true);
		this.arguments = PluginUtils.configureStringsProperty(configElem, ARGUMENT);
		this.document = PluginUtils.configureCommandDocumentProperty(configElem, DOCUMENT, false);
		if(this.arguments.size() > 0 && this.document != null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Cannot supply both arguments and document to EcmaFunction invocation");
		}
	}

	protected String getFunctionName() {
		return functionName;
	}

	protected List<String> getArguments() {
		return arguments;
	}

	protected CommandDocument getDocument() {
		return document;
	}

	
}
