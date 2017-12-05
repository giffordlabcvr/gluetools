package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.BlastSequenceRecogniserException.Code;
import uk.ac.gla.cvr.gluetools.core.blastRecogniser.RecognitionCategoryResult.Direction;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHit;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.MultiReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="blastSequenceRecogniser",
		description="Classifies sequences based on a nucleotide BLAST against a set of ReferenceSequences")
public class BlastSequenceRecogniser extends ModulePlugin<BlastSequenceRecogniser> {

	
	private static final String REFERENCE_SEQUENCE = "referenceSequence";
	private static final String RECOGNITION_CATEGORY = "recognitionCategory";
	private static final String BLAST_RUNNER = "blastRunner";
	
	private BlastRunner blastRunner = new BlastRunner();
	private List<String> refSeqNames;
	private List<RecognitionCategory> recognitionCategories;
	
	public BlastSequenceRecogniser() {
		super();
		registerModulePluginCmdClass(RecogniseFileCommand.class);
		registerModulePluginCmdClass(RecogniseSequenceCommand.class);
		registerModulePluginCmdClass(RecogniseFastaDocumentCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, BLAST_RUNNER);
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
		this.recognitionCategories = PluginFactory.createPlugins(pluginConfigContext, RecognitionCategory.class, 
				PluginUtils.findConfigElements(configElem, RECOGNITION_CATEGORY));
		this.refSeqNames = PluginUtils.configureStringsProperty(configElem, REFERENCE_SEQUENCE, 1, null);

	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		BlastDbManager.getInstance().removeMultiRefBlastDB(cmdContext, dbName());
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		Set<String> refSeqNamesSet = new LinkedHashSet<String>(refSeqNames);
		for(String refSeqName: refSeqNamesSet) {
			ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
			if(refSequence == null) {
				throw new BlastSequenceRecogniserException(Code.NO_SUCH_REFERENCE_SEQUENCE, refSeqName);
			}
		}
		Set<String> unusedRefs = new LinkedHashSet<String>(refSeqNamesSet);
		Map<String, String> refToCategory = new LinkedHashMap<String, String>();
		for(RecognitionCategory recognitionCategory: recognitionCategories) {
			String recognitionCategoryId = recognitionCategory.getId();
			List<String> categoryUsedRefs = recognitionCategory.getRefSeqNames();
			for(String categoryUsedRef: categoryUsedRefs) {
				if(!refSeqNamesSet.contains(categoryUsedRef)) {
					throw new BlastSequenceRecogniserException(Code.CATEGORY_USES_UNKNOWN_REFERENCE, recognitionCategoryId, categoryUsedRef);
				}
				String alreadyUsingCategory = refToCategory.get(categoryUsedRef);
				if(alreadyUsingCategory != null) {
					throw new BlastSequenceRecogniserException(Code.CATEGORY_REFERENCES_OVERLAP, recognitionCategoryId, alreadyUsingCategory, categoryUsedRef);
				}
				refToCategory.put(categoryUsedRef, recognitionCategoryId);
				unusedRefs.remove(categoryUsedRef);
			}
		}
		if(!unusedRefs.isEmpty()) {
			throw new BlastSequenceRecogniserException(Code.NO_CATEGORY_FOR_REFERENCE, unusedRefs.iterator().next());
		}
	}

	public Map<String, List<RecognitionCategoryResult>> recognise(CommandContext cmdContext, Map<String, DNASequence> queries) {
		LinkedHashSet<String> refNamesSet = new LinkedHashSet<String>(refSeqNames);
		Map<String, List<RecognitionCategoryResult>> queryIdToCategoryResults = new LinkedHashMap<String, List<RecognitionCategoryResult>>();
		MultiReferenceBlastDB multiReferenceDB = BlastDbManager.getInstance().ensureMultiReferenceDB(cmdContext, dbName(), refNamesSet);
		GlueLogger.getGlueLogger().finest("Executing BLAST");

		List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, BlastRunner.BlastType.BLASTN, multiReferenceDB, 
				FastaUtils.mapToFasta(queries, LineFeedStyle.forOS()));

		Map<String, RecognitionCategory> refToCategory = new LinkedHashMap<String, RecognitionCategory>();
		for(RecognitionCategory recognitionCategory: recognitionCategories) {
			List<String> categoryUsedRefs = recognitionCategory.getRefSeqNames();
			for(String categoryUsedRef: categoryUsedRefs) {
				refToCategory.put(categoryUsedRef, recognitionCategory);
			}
		}

		for(BlastResult blastResult: blastResults) {
			String queryId = blastResult.getQueryFastaId();
			Set<RecognitionCategoryResult> categoryResults = new LinkedHashSet<RecognitionCategoryResult>();
			List<BlastHit> hits = blastResult.getHits();
			for(BlastHit hit: hits) {
				String referenceName = hit.getReferenceName();
				RecognitionCategory category = refToCategory.get(referenceName);
				RecognitionCategoryResult forwardResult = new RecognitionCategoryResult(category.getId(), Direction.FORWARD);
				if(!categoryResults.contains(forwardResult)) {
					BlastHspFilter forwardHspFilter = category.getForwardHspFilter();
					List<BlastHsp> forwardHsps = hit.getHsps().stream().filter(forwardHspFilter::allowBlastHsp).collect(Collectors.toList());
					int totalAlignLen = 0;
					for(BlastHsp forwardHsp: forwardHsps) {
						totalAlignLen += forwardHsp.getAlignLen();
					}
					if(totalAlignLen >= category.getMinimumTotalAlignLength()) {
						categoryResults.add(forwardResult);
					}
				}
				RecognitionCategoryResult reverseResult = new RecognitionCategoryResult(category.getId(), Direction.REVERSE);
				if(!categoryResults.contains(reverseResult)) {
					BlastHspFilter reverseHspFilter = category.getReverseHspFilter();
					List<BlastHsp> reverseHsps = hit.getHsps().stream().filter(reverseHspFilter::allowBlastHsp).collect(Collectors.toList());
					int totalAlignLen = 0;
					for(BlastHsp reverseHsp: reverseHsps) {
						totalAlignLen += reverseHsp.getAlignLen();
					}
					if(totalAlignLen >= category.getMinimumTotalAlignLength()) {
						categoryResults.add(reverseResult);
					}
				}
			}
			queryIdToCategoryResults.put(queryId, new ArrayList<RecognitionCategoryResult>(categoryResults));
		}
		
		return queryIdToCategoryResults;
	}

	private String dbName() {
		return "blastSequenceRecogniser_"+getModuleName();
	}
	
	
	
}
