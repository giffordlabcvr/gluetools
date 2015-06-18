package uk.ac.gla.cvr.gluetools.core.command.project.importer;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.Importer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="show-importer")
@CommandClass(description="Show the configuration of a sequence importer", 
	docoptUsages={"<importerName>"}) 
public class ShowImporterCommand extends ProjectModeCommand {

	private String importerName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		importerName = PluginUtils.configureString(configElem, "importerName/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Importer importer = GlueDataObject.lookup(objContext, Importer.class, Importer.pkMap(getProjectName(), importerName));
		return new DocumentResult(importer.getConfigDoc());
	}


}
