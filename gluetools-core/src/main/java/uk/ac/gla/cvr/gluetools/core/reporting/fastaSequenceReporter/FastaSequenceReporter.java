package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

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
	
	public String targetRefNameFromFastaId(CommandContext cmdContext, String fastaId) {
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

	public AlignerResult alignToTargetReference(CommandContext cmdContext, String targetRefName, 
			String fastaID, DNASequence fastaNTSeq) {
		Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, getAlignerModuleName());
		return aligner.doAlign(cmdContext, targetRefName, fastaID, fastaNTSeq);
	}
	
	
	public List<TranslatedQueryAlignedSegment> translateNucleotides(
			CommandContext cmdContext, FeatureLocation featureLoc,
			List<QueryAlignedSegment> queryToRefSegsFeatureArea, String queryNTs) {
			
		// truncate to codon aligned
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> queryToRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, queryToRefSegsFeatureArea);

		final Translator translator = new CommandContextTranslator(cmdContext);
		
		if(queryToRefSegsCodonAligned.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<TranslatedQueryAlignedSegment> translatedQaSegs = new ArrayList<TranslatedQueryAlignedSegment>();
		
		for(QueryAlignedSegment queryToRefSeg: queryToRefSegsCodonAligned) {
			CharSequence nts = SegmentUtils.base1SubString(queryNTs, queryToRefSeg.getQueryStart(), queryToRefSeg.getQueryEnd());
			String segAAs = translator.translate(nts);
			translatedQaSegs.add(new TranslatedQueryAlignedSegment(queryToRefSeg, segAAs));
		}
		return translatedQaSegs;
	}


	
	public static class TranslatedQueryAlignedSegment {

		private QueryAlignedSegment queryAlignedSegment;
		private String translation;
		
		public TranslatedQueryAlignedSegment(QueryAlignedSegment queryAlignedSegment, String translation) {
			this.queryAlignedSegment = queryAlignedSegment;
			this.translation = translation;
		}

		public QueryAlignedSegment getQueryAlignedSegment() {
			return queryAlignedSegment;
		}

		public String getTranslation() {
			return translation;
		}
	}
	
}
