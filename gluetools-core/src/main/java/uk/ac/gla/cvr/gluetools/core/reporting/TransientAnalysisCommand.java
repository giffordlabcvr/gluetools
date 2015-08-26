package uk.ac.gla.cvr.gluetools.core.reporting;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"transient", "analysis"}, 
		description = "Analyse mutations for binary sequence data", 
		docoptUsages = { "-b <data> <sequenceFormat> <referenceName>" }, 
		docoptOptions = {
				"-b <data>, --base64 <data>  Sequence binary data"
		},
		metaTags = { CmdMeta.consumesBinary }
		
)
public class TransientAnalysisCommand extends ModuleProvidedCommand<TransientAnalysisCommand.TransientAnalysisResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {
	

	public static final String SEQUENCE_FORMAT = "sequenceFormat";
	public static final String REFERENCE_NAME = "referenceName";
	public static final String BASE_64 = Command.BINARY_INPUT_PROPERTY;
	
	private String referenceName;
	private SequenceFormat sequenceFormat;
	private byte[] sequenceData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, SEQUENCE_FORMAT, true);
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		sequenceData = PluginUtils.configureBase64BytesProperty(configElem, BASE_64, true);
	}

	@Override
	protected TransientAnalysisResult execute(CommandContext cmdContext, MutationFrequenciesReporter mutationFrequenciesPlugin) {
		return mutationFrequenciesPlugin.doTransientAnalysis(cmdContext, sequenceData, sequenceFormat, referenceName);
	}

	public static class TransientAnalysisResult extends CommandResult {

		protected TransientAnalysisResult(String nucleotides, String referenceName) {
			super("transientAnalysisResult");
			getDocumentBuilder().set("nucleotides", nucleotides);
			getDocumentBuilder().set("referenceName", referenceName);
		}
		
	}

	
}