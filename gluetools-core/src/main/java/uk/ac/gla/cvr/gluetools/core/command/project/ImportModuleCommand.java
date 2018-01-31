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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import","module"}, 
	docoptUsages={"[-r] <moduleName> <fileName>"},
	docoptOptions={"-r, --reload  If module exists, reload its config"},
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
	description="DEPRECATED: Create a new module, importing config from a file") 
public class ImportModuleCommand extends ProjectModeCommand<OkResult> {

	private String moduleName;
	private String fileName;
	private Boolean reload;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", true);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		reload = PluginUtils.configureBooleanProperty(configElem, "reload", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		GlueLogger.getGlueLogger().warning("Command \"import module\" is deprecated. To create a new module, use command \"create module\". "+
				"To update an existing module's config, use command \"import configuration\" in the relevant module's command mode.");
		Module module = null;
		boolean moduleExisted = false;
		if(reload) {
			module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName), true);
		}
		if(module != null) {
			moduleExisted = true;
		} else {
			module = GlueDataObject.create(cmdContext, Module.class, Module.pkMap(moduleName), false);
		}
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		byte[] config = consoleCmdContext.loadBytes(fileName);
		module.setConfig(config);
		try {
			module.getModulePlugin(cmdContext);
		} catch(Exception e) {
			throw new ModuleException(Code.CREATE_FROM_FILE_FAILED, fileName, e.getMessage());
		}
		cmdContext.commit();
		if(moduleExisted) {
			return new UpdateResult(Module.class, 1);
		} else {
			return new CreateResult(Module.class, 1);
		}
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("moduleName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					Object reloadObj = bindings.get("reload");
					if(reloadObj != null && ((Boolean) reloadObj)) {
						return listNames(cmdContext, prefix, Module.class, Module.NAME_PROPERTY);
					}
					return null;
				}
			});
			registerPathLookup("fileName", false);
		}
	}

	
	
	
}
