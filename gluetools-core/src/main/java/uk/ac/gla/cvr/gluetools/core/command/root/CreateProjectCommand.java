package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.Optional;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectSetSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.VersionUtils;


@CommandClass( 
	commandWords={"create", "project"}, 
	docoptUsages={"<projectName> [-n <minVersion>] [-x <maxVersion>] [<description>]"},
	docoptOptions={
		"-n <minVersion>, --minVersion <minVersion>  Min GLUE version to build project",
		"-x <maxVersion>, --maxVersion <maxVersion>  Max GLUE version to build project"},
	description="Create a new project",
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp="The project name must be a valid database identifier, e.g. my_project_1.\n"+
		"The <minVersion> and <maxVersion> properties are optional, however <minVersion> is strongly recommended. "+
		"The version strings, if used, must start with 3 positive integers separated by dots. "+
		"If min/max versions are set there is a consistency check between the project min/max versions and the GLUE engine "+
		"version at the time the project is built / rebuilt.") 
public class CreateProjectCommand extends RootModeCommand<CreateResult> {

	public static final String PROJECT_NAME = "projectName";
	public static final String DESCRIPTION = "description";
	public static final String MIN_VERSION = "minVersion";
	public static final String MAX_VERSION = "maxVersion";
	
	private String projectName;
	private Optional<String> description;
	private String minVersion;
	private String maxVersion;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		projectName = PluginUtils.configureIdentifierProperty(configElem, PROJECT_NAME, true);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, DESCRIPTION, false));
		minVersion = PluginUtils.configureStringProperty(configElem, MIN_VERSION, false);
		maxVersion = PluginUtils.configureStringProperty(configElem, MAX_VERSION, false);
		if(minVersion != null) {
			VersionUtils.parseVersionString(minVersion);
		}
		if(maxVersion != null) {
			VersionUtils.parseVersionString(maxVersion);
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		if(minVersion != null) {
			VersionUtils.checkMinVersion(cmdContext, minVersion);
		}
		if(maxVersion != null) {
			VersionUtils.checkMaxVersion(cmdContext, maxVersion);
		}

		Project newProject = GlueDataObject.create(cmdContext, Project.class, Project.pkMap(projectName), false);
		description.ifPresent(newProject::setDescription);
		ServerRuntime projectRuntime = 
				ModelBuilder.createProjectModel(cmdContext.getGluetoolsEngine(), newProject);
		projectRuntime.shutdown();
		cmdContext.commit();
		if(minVersion != null) {
			try(ModeCloser projectMode = cmdContext.pushCommandMode("project", projectName)) {
				cmdContext.cmdBuilder(ProjectSetSettingCommand.class)
				.set(ProjectSetSettingCommand.SETTING_NAME, ProjectSettingOption.MIN_ENGINE_VERSION.getName())
				.set(ProjectSetSettingCommand.SETTING_VALUE, minVersion)
				.execute();
			}
		}
		if(maxVersion != null) {
			try(ModeCloser projectMode = cmdContext.pushCommandMode("project", projectName)) {
				cmdContext.cmdBuilder(ProjectSetSettingCommand.class)
				.set(ProjectSetSettingCommand.SETTING_NAME, ProjectSettingOption.MAX_ENGINE_VERSION.getName())
				.set(ProjectSetSettingCommand.SETTING_VALUE, maxVersion)
				.execute();
			}
		}
		
		return new CreateResult(Project.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

	
}
