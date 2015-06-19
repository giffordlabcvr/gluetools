package uk.ac.gla.cvr.gluetools.core.command.project.populator;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.Populator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="show-populator")
@CommandClass(description="Show the configuration of a sequence populator", 
	docoptUsages={"<populatorName>"}) 
public class ShowPopulatorCommand extends ProjectModeCommand {

	private String populatorName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		populatorName = PluginUtils.configureString(configElem, "populatorName/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Populator populator = GlueDataObject.lookup(objContext, Populator.class, Populator.pkMap(getProjectName(), populatorName));
		return new DocumentResult(populator.getConfigDoc());
	}


}
