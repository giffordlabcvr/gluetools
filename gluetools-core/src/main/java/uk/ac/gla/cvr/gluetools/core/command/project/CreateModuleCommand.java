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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"create", "module"}, 
		docoptUsages={"(-t <moduleType> | -f <fileName> [-r] ) [<moduleName>]"},
		description="Create a module",
		docoptOptions={
				"-t <moduleType>, --moduleType <moduleType>  Default config for a specific module type", 
				"-f <fileName>, --fileName <fileName>        Config from an XML file",
				"-r, --loadResources                         Also load dependent resources"},
		furtherHelp="Creates a module either of a specified type, with the default configuration for that type, "+
				"or from an XML file, in which case the module type is specified by the document root element name."+
				"\nIf no <moduleName> is specified, a name is auto-generated from the supplied <moduleType> or from the "+
				"file part of <fileName> path, without the '.xml' extension. The module name is given a numeric suffix if "+
				"necessary to ensure it is unique. \nWARNING: The --loadResources (or -r) option is no longer necessary in "+ 
				"version 1.1.105 and later. Resources will always be loaded when applicable.\n",
		metaTags={ CmdMeta.consoleOnly } ) 
	
public class CreateModuleCommand extends Command<CreateResult> {

	public static final String MODULE_TYPE = "moduleType";
	public static final String FILE_NAME = "fileName";
	public static final String MODULE_NAME = "moduleName";
	private static final String LOAD_RESOURCES = "loadResources";

	private String moduleType;
	private String moduleName;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.moduleType = PluginUtils.configureStringProperty(configElem, MODULE_TYPE, false);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		this.moduleName = PluginUtils.configureStringProperty(configElem, MODULE_NAME, false);
		boolean loadResources = PluginUtils.configureBooleanProperty(configElem, LOAD_RESOURCES, true);
		
		if(moduleType == null && fileName == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <moduleType> or <fileName> must be specified.");
		}
		if(moduleType != null && fileName != null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Only one of <moduleType> or <fileName> may be specified.");
		}
		if(fileName != null && !fileName.endsWith(".xml")) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <fileName> must end with .xml");
		}
		if(loadResources) {
			GlueLogger.getGlueLogger().warning("The --loadResources (or -r) option is no longer necessary in version 1.1.105 and later. Resources will always be loaded when applicable.");
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		byte[] config = null;
		
		String nameToUse = moduleName;
		if(nameToUse == null) {
			String namePrefix;
			if(moduleType != null) {
				namePrefix = moduleType;
			} else {
				File file = new File(fileName);
				String filePart = file.getName();
				namePrefix = filePart.substring(0, filePart.length()-4); // remove ".xml"
			}
			String nameToTry = null;
			int suffix = 1;
			while(true) {
				if(nameToTry == null) {
					nameToTry = namePrefix;
				} else {
					nameToTry = namePrefix+Integer.toString(suffix);
					suffix++;
				}
				Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(nameToTry), true);
				if(module == null) {
					nameToUse = nameToTry;
					break;
				}
			}
		}
		Module module = GlueDataObject.create(cmdContext, Module.class, Module.pkMap(nameToUse), false);
		if(moduleType != null) {
			ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
			if(!pluginFactory.getElementNames().contains(moduleType)) {
				throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_TYPE, moduleType);
			}
			
			// create document with just a single type element in it.
			Document document = GlueXmlUtils.newDocument();
			document.appendChild(document.createElement(moduleType));
			config = GlueXmlUtils.prettyPrint(document);
			module.setConfig(config);
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			module.loadConfig(consoleCmdContext, fileName);
		}
		cmdContext.commit();
		GlueLogger.log(Level.FINEST, "Created new module '"+nameToUse+"' of type "+module.getType());
		return new CreateResult(Module.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("moduleType", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					ModulePluginFactory pluginFactory = PluginFactory.get(ModulePluginFactory.creator);
					return pluginFactory.getElementNames().stream()
							.map(eName -> new CompletionSuggestion(eName, true))
							.collect(Collectors.toList());
				}
			});
			registerPathLookup("fileName", false);
		}
		
	}
	
}