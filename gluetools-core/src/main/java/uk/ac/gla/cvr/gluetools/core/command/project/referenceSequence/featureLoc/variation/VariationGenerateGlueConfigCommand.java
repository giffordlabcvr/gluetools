package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.GlueConfigResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"generate", "glue-config"},
		docoptUsages={"[-f <fileName>]"},
		docoptOptions={ 
				"-f <fileName>, --fileName <fileName>  Name of file to output to"},
		description="Generate GLUE configuration to recreate the variation",
		furtherHelp="If a <fileName> is supplied, GLUE commands will be saved to that file. "+
		"Otherwise they will be output to the console.",
		metaTags={ CmdMeta.consoleOnly }
)
public class VariationGenerateGlueConfigCommand extends VariationModeCommand<GlueConfigResult> {
	
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", false);
	}



	@Override
	public GlueConfigResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		StringBuffer glueConfigBuf = new StringBuffer();
		Variation variation = lookupVariation(consoleCmdContext);
		variation.generateGlueConfig(0, glueConfigBuf);
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
