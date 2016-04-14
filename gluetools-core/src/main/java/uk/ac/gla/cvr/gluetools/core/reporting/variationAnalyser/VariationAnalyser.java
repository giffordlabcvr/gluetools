package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="variationAnalyser")
public class VariationAnalyser extends ModulePlugin<VariationAnalyser> {

	
	
	public static final String FASTA_SEQUENCE_REPORTER_MODULE_NAME = "fastaSequenceReporterModuleName";

	private String fastaSequenceReporterModuleName;
	
	public VariationAnalyser() {
		super();
		addModulePluginCmdClass(WebVariationAnalysisCommand.class);
		addModulePluginCmdClass(VariationAnalysisCommand.class);
		addSimplePropertyName(FASTA_SEQUENCE_REPORTER_MODULE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fastaSequenceReporterModuleName = PluginUtils.configureStringProperty(configElem, FASTA_SEQUENCE_REPORTER_MODULE_NAME, true);
	}

	public VariationAnalysis analyse(CommandContext cmdContext, byte[] fastaBytes) {
		
		FastaSequenceReporter fastaSequenceReporter = 
				Module.resolveModulePlugin(cmdContext, FastaSequenceReporter.class, fastaSequenceReporterModuleName);
		
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> fastaIdToSequence = FastaUtils.parseFasta(fastaBytes);
		List<SequenceAnalysis> sequenceAnalysisList = new ArrayList<SequenceAnalysis>();
		fastaIdToSequence.forEach((fastaId, sequence) -> {
			String targetRefName = fastaSequenceReporter.targetRefNameFromFastaId(cmdContext, fastaId);
			
			
			sequenceAnalysisList.add(new SequenceAnalysis(fastaId, targetRefName));
		});
		return new VariationAnalysis(sequenceAnalysisList);
	}

}
