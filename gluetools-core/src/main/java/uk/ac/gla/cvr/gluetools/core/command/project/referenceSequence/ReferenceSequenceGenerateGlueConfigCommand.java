package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.GlueConfigResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"generate", "glue-config"},
		docoptUsages={"-v [-f <fileName>]"},
		docoptOptions={
				"-v, --variations                      Generate feature location variations", 
				"-f <fileName>, --fileName <fileName>  Name of file to output to"},
		description="Generate GLUE configuration to recreate the reference sequence",
		furtherHelp="If a <fileName> is supplied, GLUE commands will be saved to that file. "+
		"Otherwise they will be output to the console.",
		metaTags={ CmdMeta.consoleOnly }
)
public class ReferenceSequenceGenerateGlueConfigCommand extends ReferenceSequenceModeCommand<GlueConfigResult> {
	
	private String fileName;
	private boolean variations;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", false);
		variations = PluginUtils.configureBooleanProperty(configElem, "variations", true);
	}

	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		ReferenceSequence refSequence = lookupRefSeq(cmdContext);
		GlueConfigContext glueConfigContext = new GlueConfigContext(cmdContext);
		glueConfigContext.setIncludeVariations(variations);
		return GlueConfigResult.generateGlueConfigResult(cmdContext, fileName, refSequence.generateGlueConfig(glueConfigContext));
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}
	
}
