package uk.ac.gla.cvr.gluetools.core.command.project.populator;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.PopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.Populator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="run-populator")
@CommandClass(description="Run a data field populator", 
	docoptUsages={"<populatorName> <sourceName>"}) 
public class RunPopulatorCommand extends ProjectModeCommand {

	private String populatorName;
	private String sourceName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		populatorName = PluginUtils.configureString(configElem, "populatorName/text()", true);
		sourceName = PluginUtils.configureString(configElem, "sourceName/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Populator populator = GlueDataObject.lookup(objContext, Populator.class, Populator.pkMap(getProjectName(), populatorName));
		PopulatorPlugin populatorPlugin = populator.getPopulatorPlugin(cmdContext.getGluetoolsEngine().createPluginConfigContext());
		return populatorPlugin.populate(cmdContext, sourceName);
	}


}
