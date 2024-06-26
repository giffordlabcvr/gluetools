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
package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(commandWords={"load", "configuration"},
	docoptUsages="<fileName> [-r]",
	docoptOptions={
		"-r, --loadResources  Also load dependent resources",
	},
	description = "Load module configuration from a file", 
	furtherHelp="\nWARNING: The --loadResources (or -r) option is no longer necessary in "+ 
			"version 1.1.105 and later. Resources will always be loaded when applicable.\n",
	metaTags = { CmdMeta.consoleOnly} )
public class ModuleLoadConfigurationCommand extends ModuleDocumentCommand<UpdateResult> {

	private static final String FILE_NAME = "fileName";
	private static final String LOAD_RESOURCES = "loadResources";
	private String fileName;
	
	public ModuleLoadConfigurationCommand() {
		super();
		setRequireValidCurrentDocument(false);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		boolean loadResources = PluginUtils.configureBooleanProperty(configElem, LOAD_RESOURCES, true);
		
		if(loadResources) {
			GlueLogger.getGlueLogger().warning("The --loadResources (or -r) option is no longer necessary in version 1.1.105 and later. Resources will always be loaded when applicable.");
		}
	}

	// do the commit here rather than implementing the ModuleUpdateDocumentCommand marker interface.
	@Override
	protected UpdateResult processDocument(CommandContext cmdContext,
			Module module, Document modulePluginDoc) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		module.loadConfig(consoleCmdContext, fileName);
		consoleCmdContext.commit();
		return new UpdateResult(Module.class, 1);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}


}
