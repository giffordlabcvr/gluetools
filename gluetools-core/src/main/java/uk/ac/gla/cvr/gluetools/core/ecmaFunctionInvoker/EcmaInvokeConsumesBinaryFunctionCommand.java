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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"invoke-consumes-binary-function"}, 
		description = "Invoke an ECMAScript function", 
		docoptUsages = { "-b <inputData> <functionName> [<argument> ...]" },
		docoptOptions = {
				"-b <inputData>, --base64 <inputData>  Input data"
		},
		metaTags = { CmdMeta.webApiOnly, CmdMeta.consumesBinary },
		furtherHelp = ""
)
public class EcmaInvokeConsumesBinaryFunctionCommand extends EcmaFunctionCommand implements ProvidedProjectModeCommand {

	private String inputData;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputData = PluginUtils.configureStringProperty(configElem, BINARY_INPUT_PROPERTY, true);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, EcmaFunctionInvoker ecmaFunctionInvoker) {
		EcmaFunction function = ecmaFunctionInvoker.getFunction(getFunctionName());
		if(!function.consumesBinary()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), function.getName(), 
					"Command 'invoke-consumes-binary-function' may only be set on ECMA functions with the consumesBinary flag set to true.");
		}
		List<String> otherArguments = getArguments();
		ArrayList<String> arguments = new ArrayList<String>();
		arguments.add(inputData);
		arguments.addAll(otherArguments);
		return ecmaFunctionInvoker.invokeFunction(cmdContext, getFunctionName(), arguments, getDocument());
	}

	
}
