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
package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"add", "variation", "field-setting"}, 
		description = "Add a custom field setting for generated variations", 
		docoptUsages={"<fieldName> <fieldValue>"},
		docoptOptions={},
		metaTags = {CmdMeta.updatesDatabase}, 
		furtherHelp = "The setting will be applied to all variations which the module generates"
)
public class AddVariationFieldSettingCommand extends ModuleDocumentCommand<OkResult> implements ModuleUpdateDocumentCommand {

	private String fieldName;
	private String fieldValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		fieldValue = PluginUtils.configureStringProperty(configElem, "fieldValue", true);
	}

	@Override
	protected OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		insideProjectMode.getProject().checkCustomFieldNames(ConfigurableTable.variation.name(), Arrays.asList(fieldName));
		Element customFieldSettingElem = GlueXmlUtils.appendElement(modulePluginDoc.getDocumentElement(), "variationCustomFieldSetting");
		GlueXmlUtils.appendElementWithText(customFieldSettingElem, "fieldName", fieldName);
		GlueXmlUtils.appendElementWithText(customFieldSettingElem, "fieldValue", fieldValue);
		return new OkResult();
	}
	
	@SuppressWarnings("rawtypes")
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
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

}
