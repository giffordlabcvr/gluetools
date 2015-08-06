package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class TableSequencesModeCommand<R extends CommandResult> extends Command<R> {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	protected String getProjectName() {
		return projectName;
	}
		
}
