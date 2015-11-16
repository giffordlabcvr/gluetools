package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.GlueConfigResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"generate", "glue-config"},
		docoptUsages={"-v [-f <fileName>]"},
		docoptOptions={
				"-v, --variationsOnly                  Only generate feature location variations", 
				"-f <fileName>, --fileName <fileName>  Name of file to output to"},
		description="Generate GLUE configuration to recreate the reference sequence",
		furtherHelp="If a <fileName> is supplied, GLUE commands will be saved to that file. "+
		"Otherwise they will be output to the console.",
		metaTags={ CmdMeta.consoleOnly }
)
public class ReferenceSequenceGenerateGlueConfigCommand extends ReferenceSequenceModeCommand<GlueConfigResult> {
	
	private String fileName;
	private boolean variationsOnly;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", false);
		variationsOnly = PluginUtils.configureBooleanProperty(configElem, "variationsOnly", true);
	}

	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		StringBuffer glueConfigBuf = new StringBuffer();
		ReferenceSequence refSequence = lookupRefSeq(consoleCmdContext);
		refSequence.generateGlueConfig(0, glueConfigBuf, variationsOnly);
		boolean outputToConsole = false;
		String glueConfig = glueConfigBuf.toString();
		if(fileName == null) {
			outputToConsole = true;
		} else {
			consoleCmdContext.saveBytes(fileName, glueConfig.getBytes());
		}
		GlueConfigResult glueConfigResult = new GlueConfigResult(outputToConsole, glueConfig);
		return glueConfigResult;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}
	
}
