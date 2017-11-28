package uk.ac.gla.cvr.gluetools.core.command.root;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.ProjectSchemaMode;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.ProjectSchemaModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"schema-project"},
	docoptUsages={"<projectName>"},
	description="Enter command mode to manage a project's schema")
@EnterModeCommandClass(
		commandFactoryClass = ProjectSchemaModeCommandFactory.class, 
		modeDescription = "Schema mode")
public class ProjectSchemaCommand extends RootModeCommand<OkResult>  {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(projectName));
		cmdContext.pushCommandMode(new ProjectSchemaMode(cmdContext, this, project));
		return CommandResult.OK;
	}
	
	@CompleterClass
	public static class Completer extends ProjectNameCompleter {}


}
