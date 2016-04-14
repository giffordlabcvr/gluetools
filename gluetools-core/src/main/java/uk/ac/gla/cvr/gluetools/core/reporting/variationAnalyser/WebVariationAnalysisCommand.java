package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

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
		commandWords={"web-variation-analysis"}, 
		description = "Run web variation analysis", 
		docoptUsages = { "-b <inputData>" },
		docoptOptions = { "-b <inputData>, --base64 <inputData>  Multi-FASTA input data"},
		metaTags = {CmdMeta.webApiOnly, CmdMeta.consumesBinary}	
)
public class WebVariationAnalysisCommand extends ModulePluginCommand<PojoCommandResult<VariationAnalysis>, VariationAnalyser> 
	implements ProvidedProjectModeCommand {

	private byte[] inputData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputData = PluginUtils.configureBase64BytesProperty(configElem, BINARY_INPUT_PROPERTY, true);
	}

	@Override
	protected PojoCommandResult<VariationAnalysis> execute(CommandContext cmdContext, VariationAnalyser variationAnalyser) {
		return new PojoCommandResult<VariationAnalysis>(variationAnalyser.analyse(cmdContext, inputData));
	}
	
}
