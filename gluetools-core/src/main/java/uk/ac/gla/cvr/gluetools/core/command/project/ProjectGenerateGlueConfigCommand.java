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
		docoptUsages={"[-v] [-c] [-f <fileName>]"},
		docoptOptions={
				"-v, --variations                      Include reference sequence feature variations",
				"-c, --variationCategories             Include variation categories",
				"-f <fileName>, --fileName <fileName>  Name of file to output to"},
		description="Generate GLUE configuration to recreate the reference sequence",
		furtherHelp="If a <fileName> is supplied, GLUE commands will be saved to that file. "+
		"Otherwise they will be output to the console.",
		metaTags={ CmdMeta.consoleOnly }
)
public class ProjectGenerateGlueConfigCommand extends ProjectModeCommand<GlueConfigResult> {
	
	private String fileName;
	private boolean variations;
	private boolean variationCategories;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", false);
		variations = PluginUtils.configureBooleanProperty(configElem, "variations", true);
		variationCategories = PluginUtils.configureBooleanProperty(configElem, "variationCategories", true);
	}

	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		GlueConfigContext glueConfigContext = new GlueConfigContext(cmdContext);
		glueConfigContext.setIncludeVariations(variations);
		glueConfigContext.setIncludeVariationCategories(variationCategories);
		return GlueConfigResult.generateGlueConfigResult(cmdContext, fileName, project.generateGlueConfig(glueConfigContext));
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

}
