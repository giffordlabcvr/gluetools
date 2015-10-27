package uk.ac.gla.cvr.gluetools.core.command.root;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "project"},
	docoptUsages={"<projectName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a project") 
public class DeleteProjectCommand extends RootModeCommand<DeleteResult> {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(projectName), true);
		if(project == null) {
			return new DeleteResult(Project.class, 0);
		}
		ModelBuilder.deleteProjectModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), project);
		DeleteResult result = GlueDataObject.delete(cmdContext, Project.class, Project.pkMap(projectName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends ProjectNameCompleter {}


}
