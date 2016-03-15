package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;

@PluginClass(elemName="fastaSequenceReporter")
public class FastaSequenceReporter extends ModulePlugin<FastaSequenceReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String FASTA_ID_TEXT_TO_REFERENCE_QUERY_MODULE_NAME = "fastaIdTextToReferenceQueryModuleName";


	private String alignerModuleName;
	// Transforms FASTA ID to a where clause identifying the target reference.
	private String fastaIdTextToReferenceQueryModuleName;


	public FastaSequenceReporter() {
		super();
		addModulePluginCmdClass(FastaSequenceAminoAcidCommand.class);
		addModulePluginCmdClass(FastaSequenceVariationScanCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);
		addSimplePropertyName(FASTA_ID_TEXT_TO_REFERENCE_QUERY_MODULE_NAME);

	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
		this.fastaIdTextToReferenceQueryModuleName = PluginUtils.configureStringProperty(configElem, FASTA_ID_TEXT_TO_REFERENCE_QUERY_MODULE_NAME, false);
	}
	
	public String getAlignerModuleName() {
		return alignerModuleName;
	}
	
	public String targetRefNameFromFastaId(CommandContext cmdContext, String fastaId, String definedTargetRefName) {
		if(definedTargetRefName != null) {
			return definedTargetRefName;
		}
		if(fastaIdTextToReferenceQueryModuleName == null) {
			throw new FastaSequenceException(Code.NO_TARGET_REFERENCE_DEFINED);
		}
		TextToQueryTransformer fastaIdTextToReferenceQueryTransformer = 
				TextToQueryTransformer.lookupTextToQueryTransformer(cmdContext, fastaIdTextToReferenceQueryModuleName,
						TextToQueryTransformer.DataClassEnum.ReferenceSequence);
		List<String> referenceSeqNames = fastaIdTextToReferenceQueryTransformer.textToQuery(cmdContext, fastaId).
				getColumnValues(ReferenceSequence.NAME_PROPERTY);
		if(referenceSeqNames.size() == 0) {
			throw new FastaSequenceException(Code.TARGET_REFERENCE_NOT_FOUND, fastaId);
		}
		if(referenceSeqNames.size() > 1) {
			throw new FastaSequenceException(Code.TARGET_REFERENCE_AMBIGUOUS, fastaId, referenceSeqNames.toString());
		}
		return referenceSeqNames.get(0);
	}

	
	
}
