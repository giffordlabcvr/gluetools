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
package uk.ac.gla.cvr.gluetools.core.command.project.module.property;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class ModulePropertyCommand<R extends CommandResult> extends ModuleDocumentCommand<R> {

	public static final String PROPERTY_PATH = "propertyPath";
	
	private String propertyPath;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.propertyPath = PluginUtils.configureStringProperty(configElem, PROPERTY_PATH, true);
	}

	protected void checkPropertyPath(CommandContext cmdContext, Module module) {
		ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext, false);
		modulePlugin.checkProperty(getPropertyPath());
	}

	protected void checkPropertyGroup(CommandContext cmdContext, Module module) {
		ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext, false);
		modulePlugin.checkPropertyGroup(getPropertyPath());
	}

	protected String getPropertyPath() {
		return propertyPath;
	}
	
	protected String resolveElemName() {
		List<String> propertyPathElems = Arrays.asList(getPropertyPath().split("/"));
		String elemName = propertyPathElems.get(propertyPathElems.size()-1);
		return elemName;
	}

	protected Element resolveParentElem(Document modulePluginDoc) {
		List<String> propertyPathElems = Arrays.asList(getPropertyPath().split("/"));
		Element parentElem = modulePluginDoc.getDocumentElement();
		if(propertyPathElems.size() > 1) {
			String parentXPath = String.join("/", propertyPathElems.subList(0, propertyPathElems.size()-1));
			parentElem = GlueXmlUtils.getXPathElement(parentElem, parentXPath);
			if(parentElem == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Parent property group "+parentXPath+" does not exist");
			}
		}
		return parentElem;
	}

	
	@SuppressWarnings("rawtypes")
	public static class PropertyNameCompleter extends AdvancedCmdCompleter {
		public PropertyNameCompleter() {
			super();
			registerVariableInstantiator("propertyPath", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String moduleName = ((ModuleMode) cmdContext.peekCommandMode()).getModuleName();
					Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
					return module.getModulePlugin(cmdContext, false).allPropertyPaths()
							.stream()
							.map(pn -> new CompletionSuggestion(pn, true))
							.collect(Collectors.toList());
				}
			});
		}
	}

	@SuppressWarnings("rawtypes")
	public static class PropertyGroupNameCompleter extends AdvancedCmdCompleter {
		public PropertyGroupNameCompleter() {
			super();
			registerVariableInstantiator("propertyPath", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String moduleName = ((ModuleMode) cmdContext.peekCommandMode()).getModuleName();
					Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
					return module.getModulePlugin(cmdContext, false).allPropertyGroupPaths()
							.stream()
							.map(pn -> new CompletionSuggestion(pn, true))
							.collect(Collectors.toList());
				}
			});
		}
	}


	
}
