package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"analysis"}, 
		description = "Run analysis from the command line", 
		docoptUsages = { "-i <fileName>" },
		docoptOptions = { "-i <fileName>, --fileName <fileName>  Multi-FASTA file to analyse"},
		metaTags = {CmdMeta.consoleOnly}	
)
public class AnalysisCommand extends ModulePluginCommand<PojoCommandResult<WebAnalysisResult>, WebAnalysisTool> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";

	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}


	@Override
	protected PojoCommandResult<WebAnalysisResult> execute(CommandContext cmdContext, WebAnalysisTool variationAnalyser) {
		byte[] fileContent = ((ConsoleCommandContext) cmdContext).loadBytes(fileName);
		return new PojoCommandResult<WebAnalysisResult>(variationAnalyser.analyse(cmdContext, fileContent));
	}
	
	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
