package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesReporter.ReferenceResult;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesReporter.SequenceResult;

@CommandClass(
		commandWords={"transient", "analysis"}, 
		description = "Analyse mutations for binary sequence data", 
		docoptUsages = { "-b <data> (-h | <referenceName>)" }, 
		docoptOptions = {
				"-h, --headerDetect          Guess reference from sequence header",
				"-b <data>, --base64 <data>  Sequence binary data"
		},
		metaTags = { CmdMeta.consumesBinary }
		
)
public class TransientAnalysisCommand extends ModuleProvidedCommand<TransientAnalysisCommand.TransientAnalysisResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {
	

	public static final String HEADER_DETECT = "headerDetect";
	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String BASE_64 = Command.BINARY_INPUT_PROPERTY;
	
	private Optional<String> alignmentName;
	private Boolean headerDetect;
	private byte[] sequenceData;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		headerDetect = PluginUtils.configureBooleanProperty(configElem, HEADER_DETECT, true);
		alignmentName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, false));
		sequenceData = PluginUtils.configureBase64BytesProperty(configElem, BASE_64, true);
		if(!( 
				(alignmentName.isPresent() && !headerDetect) || 
				(!alignmentName.isPresent() && headerDetect)  
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <referenceName> or <headerDetect> must be specified, but not both");
	}

	@Override
	protected TransientAnalysisResult execute(CommandContext cmdContext, MutationFrequenciesReporter mutationFrequenciesPlugin) {
		return mutationFrequenciesPlugin.doTransientAnalysis(cmdContext, headerDetect, alignmentName, sequenceData);
	}

	public static class TransientAnalysisResult extends CommandResult {

		protected TransientAnalysisResult(List<ReferenceResult> refResults, List<SequenceResult> seqResults) {
			super("transientAnalysisResult");
			ArrayBuilder refResultArrayBuilder = getDocumentBuilder().setArray("referenceResult");
			for(ReferenceResult refResult: refResults) {
				refResult.toDocument(refResultArrayBuilder.addObject());
			}
			ArrayBuilder seqResultArrayBuilder = getDocumentBuilder().setArray("sequenceResult");
			for(SequenceResult seqResult: seqResults) {
				seqResult.toDocument(seqResultArrayBuilder.addObject());
			}
		}
		
	}


	
}