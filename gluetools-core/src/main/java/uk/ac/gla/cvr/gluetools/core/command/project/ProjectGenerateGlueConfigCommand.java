package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.GlueConfigResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"generate", "glue-config"},
		docoptUsages={"[-v] [-C] [-e] (-f <fileName> | -p)"},
		docoptOptions={
				"-v, --variations                      Include variations",
				"-C, --noCommit                        Generated commands should not commit",
				"-e, --commitAtEnd                     Add commit command at end",
				"-f <fileName>, --fileName <fileName>  Name of file to output to",
				"-p, --preview                         Preview only"},
		description="Generate GLUE configuration to recreate certain project objects",
		metaTags={ CmdMeta.consoleOnly }
)
public class ProjectGenerateGlueConfigCommand extends ProjectModeCommand<GlueConfigResult> {
	
	public static final String VARIATIONS = "variations";
	
	private GenerateConfigCommandDelegate generateConfigCommandDelegate = new GenerateConfigCommandDelegate();
	
	private boolean includeVariations;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		includeVariations = PluginUtils.configureBooleanProperty(configElem, VARIATIONS, true);
		generateConfigCommandDelegate.configure(pluginConfigContext, configElem);
	}

	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		GlueConfigContext glueConfigContext = new GlueConfigContext(cmdContext, includeVariations, 
				generateConfigCommandDelegate.getNoCommit(), 
				generateConfigCommandDelegate.getCommitAtEnd());
		return GlueConfigResult.generateGlueConfigResult(cmdContext, 
				generateConfigCommandDelegate.getPreview(), 
				generateConfigCommandDelegate.getFileName(), 
				project.generateGlueConfig(glueConfigContext));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

}
