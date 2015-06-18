package uk.ac.gla.cvr.gluetools.core.command.console;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="set-directory")
@CommandClass(
		description = "Change the directory for loading and saving",
		docoptUsages = {
				"<directory>"
		})
public class SetDirectoryCommand extends ConsoleCommand {

	private String directory;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		directory = PluginUtils.configureString(configElem, "directory/text()", true);
	}

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		return CommandResult.OK;
	}

}