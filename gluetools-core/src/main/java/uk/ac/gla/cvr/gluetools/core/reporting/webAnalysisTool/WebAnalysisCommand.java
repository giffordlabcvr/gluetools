package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"web-analysis"}, 
		description = "Run analysis from the web", 
		docoptUsages = { "-b <inputData>" },
		docoptOptions = { "-b <inputData>, --base64 <inputData>  Multi-FASTA input data"},
		metaTags = {CmdMeta.webApiOnly, CmdMeta.consumesBinary}	
)
public class WebAnalysisCommand extends ModulePluginCommand<PojoCommandResult<WebAnalysisResult>, WebAnalysisTool> 
	implements ProvidedProjectModeCommand {

	private byte[] inputData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputData = PluginUtils.configureBase64BytesProperty(configElem, BINARY_INPUT_PROPERTY, true);
	}

	@Override
	protected PojoCommandResult<WebAnalysisResult> execute(CommandContext cmdContext, WebAnalysisTool variationAnalyser) {
		return new PojoCommandResult<WebAnalysisResult>(variationAnalyser.analyse(cmdContext, inputData));
	}
	
}