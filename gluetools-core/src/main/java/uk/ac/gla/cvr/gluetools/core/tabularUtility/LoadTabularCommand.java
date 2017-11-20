package uk.ac.gla.cvr.gluetools.core.tabularUtility;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility.TabularData;

@CommandClass(
		commandWords={"load-tabular"}, 
		description = "Load tabular data from a file", 
		docoptUsages = { "<fileName>" }, 
		docCategory = "Type-specific module commands",
		metaTags = {CmdMeta.consoleOnly}	
)
public class LoadTabularCommand extends ModulePluginCommand<TabularResult, TabularUtility>{

	private static final String FILE_NAME = "fileName";

	private String fileName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	
	@Override
	protected TabularResult execute(CommandContext cmdContext, TabularUtility tabularUtility) {
		byte[] bytes = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		TabularData tabularData = tabularUtility.tabularDataFromBytes(bytes);
		return new TabularResult(tabularData);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
