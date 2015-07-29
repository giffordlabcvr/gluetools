package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "project"},
	docoptUsages={"<projectName>"},
	description="Delete a project") 
public class DeleteProjectCommand extends RootModeCommand {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Project project = getProject(objContext, projectName);
		ModelBuilder.deleteProjectModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), project);
		DeleteResult result = GlueDataObject.delete(objContext, Project.class, Project.pkMap(projectName));
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends ProjectNameCompleter {}


}
