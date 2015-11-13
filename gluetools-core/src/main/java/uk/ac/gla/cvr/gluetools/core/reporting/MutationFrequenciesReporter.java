package uk.ac.gla.cvr.gluetools.core.reporting;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ListAlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand.TranslateSegmentsResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentShowAncestorsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceRealisedFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusMinorityVariantFilter;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.AlignmentAnalysisCommand.AlignmentAnalysisResult;
import uk.ac.gla.cvr.gluetools.core.reporting.AlignmentVariationScanCommand.AlignmentVariationScanResult;
import uk.ac.gla.cvr.gluetools.core.reporting.TransientAnalysisCommand.TransientAnalysisResult;
import uk.ac.gla.cvr.gluetools.core.reporting.contentNotes.ReferenceDifferenceNote;
import uk.ac.gla.cvr.gluetools.core.reporting.contentNotes.VariationNote;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import freemarker.core.ParseException;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

@PluginClass(elemName="mutationFrequenciesReporter")
public class MutationFrequenciesReporter extends ModulePlugin<MutationFrequenciesReporter> {

	public static final String MERGE_GENERATED_VARIATIONS = "mergeGeneratedVariations";
	public static final String GENERATED_VARIATION_PERCENT_THRESHOLD = "generatedVariationPercentThreshold";
	public static final String GENERATED_VARIATION_NAME_TEMPLATE = "generatedVariationNameTemplate";

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	
	// transient analysis related.
	private String alignerModuleName;
	private Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter;

	// variation generation related
	private Template generatedVariationNameTemplate;
	private Double generatedVariationPercentThreshold;
	private boolean mergeGeneratedVariations;
	
	public MutationFrequenciesReporter() {
		addProvidedCmdClass(TransientAnalysisCommand.class);
		addProvidedCmdClass(AlignmentAnalysisCommand.class);
		addProvidedCmdClass(AlignmentVariationScanCommand.class);
		addProvidedCmdClass(GenerateVariationsCommand.class);
		addProvidedCmdClass(PreviewVariationsCommand.class);
		addProvidedCmdClass(ShowMutationFrequenciesReporterCommand.class);
		addProvidedCmdClass(ConfigureMutationFrequenciesReporterCommand.class);

	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, true);
		s2cMinorityVariantFilter = new Sam2ConsensusMinorityVariantFilter();
		Element s2cMinorityVariantFilterElem = PluginUtils.findConfigElement(configElem, "sam2ConsensusMinorityVariantFilter");
		if(s2cMinorityVariantFilterElem != null) {
			s2cMinorityVariantFilter.configure(pluginConfigContext, s2cMinorityVariantFilterElem);
		}
		Template defaultTemplate = null;
		try {
			defaultTemplate = PluginUtils.templateFromString("${refAA}_${codon}_${mutAA}", pluginConfigContext.getFreemarkerConfiguration());
		} catch(ParseException pe) {
			throw new RuntimeException(pe);
		}
		generatedVariationNameTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, GENERATED_VARIATION_NAME_TEMPLATE, false))
				.orElse(defaultTemplate);
		generatedVariationPercentThreshold = PluginUtils.configureDoubleProperty(configElem, GENERATED_VARIATION_PERCENT_THRESHOLD, 1.0);
		mergeGeneratedVariations = Optional.
				ofNullable(PluginUtils.configureBooleanProperty(configElem, MERGE_GENERATED_VARIATIONS, false)).orElse(true);
	}

	public TransientAnalysisResult doTransientAnalysis(CommandContext cmdContext,
			Boolean headerDetect, Optional<String> alignmentName, byte[] sequenceData) {

		List<AbstractSequenceObject> seqObjects = FastaUtils.seqObjectsFromSeqData(sequenceData);
		
		// initialize a sequence result for each sequence object, including
		// selecting an initial alignment
		List<SequenceResult> seqResults = initSequenceResults(cmdContext, headerDetect, alignmentName, seqObjects);
		
		// generate alignment results for all alignments involved
		Map<String, AlignmentResult> almtNameToAlmtResult = new LinkedHashMap<String, AlignmentResult>();
		addAlmtResultsFromSeqResults(almtNameToAlmtResult, cmdContext, seqResults);
		
		// generate the initial segments aligning each sequence to the selected reference.
		generateInitialAlignments(cmdContext, seqResults, almtNameToAlmtResult);
		
		// for each sequence fill in seqToRefAlignedSegments for the rest of the path to the root of the tree.
		seqResults.forEach(seqResult -> propagateAlignedSegments(cmdContext, almtNameToAlmtResult, seqResult));
		
		// for each sequence, and each alignment in its path, generate SequenceFeatureResults.
		seqResults.forEach(seqResult -> generateSequenceFeatureResults(cmdContext, almtNameToAlmtResult, seqResult, null, null));
		
		return new TransientAnalysisResult(new ArrayList<AlignmentResult>(almtNameToAlmtResult.values()), seqResults);
	}

	public void generateInitialAlignments(CommandContext cmdContext,
			List<SequenceResult> seqResults,
			Map<String, AlignmentResult> almtNameToAlmtResult) {
		// group sequence results by the name of the initial alignment.
		Map<String, List<SequenceResult>> initialAlmtNameToSeqResults = 
				seqResults.stream().collect(Collectors.groupingBy(seqResult -> 
					seqResult.getInitialAlignmentName()));
		
		// for each list of sequences which have the same starting alignment, align them to the reference of that alignment.
		// this produces seqToRefAlignedSegments for the first alignment analysis object in the chain.
		initialAlmtNameToSeqResults.forEach((almtName, almtSeqResults) -> {
			initSeqToRefAlignedSegments(cmdContext, almtSeqResults, almtNameToAlmtResult.get(almtName));
		});
	}

	public void addAlmtResultsFromSeqResults(
			Map<String, AlignmentResult> almtNameToAlmtResult,
			CommandContext cmdContext, List<SequenceResult> seqResults) {
		// collect together all alignments which will be involved, mapping their names to the relevant alignmentResults
		seqResults.forEach(seqResult -> {
			for(int i = 0; i < seqResult.seqAlignmentResults.size(); i++) {
				SequenceAlignmentResult alignmentAnalysis = seqResult.seqAlignmentResults.get(i);
				String almtName = alignmentAnalysis.getAlignmentName();
				if(!almtNameToAlmtResult.containsKey(almtName)) {
					AlignmentResult alignmentResult = new AlignmentResult(almtName);
					String parentAlignmentName = null;
					if(i < seqResult.seqAlignmentResults.size()-1) {
						parentAlignmentName = seqResult.seqAlignmentResults.get(i+1).getAlignmentName();
					}
					alignmentResult.init(cmdContext, parentAlignmentName);
					almtNameToAlmtResult.put(almtName, alignmentResult);
				}
			}});
	}


	private void initSequenceAlignmentResults(CommandContext cmdContext, List<SequenceResult> seqResults) {
		// set up each SequenceResult's list of SequenceAlignmentResults. 
		// we cache the path of alignment ancestors for each alignment.
		Map<String, List<Map<String,Object>>> almtNameToAncestorsListOfMaps 
			= new LinkedHashMap<String, List<Map<String,Object>>>();
		seqResults.forEach(seqResult -> {
			String initialAlignmentName = seqResult.getInitialAlignmentName();
			List<Map<String,Object>> ancestorsListOfMaps = almtNameToAncestorsListOfMaps.get(initialAlignmentName);
			if(ancestorsListOfMaps == null) {
				try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", initialAlignmentName)) {
					ancestorsListOfMaps = cmdContext.cmdBuilder(AlignmentShowAncestorsCommand.class).execute().asListOfMaps();
				}
				almtNameToAncestorsListOfMaps.put(initialAlignmentName, ancestorsListOfMaps);
			}
			seqResult.initSeqAlmtResults(ancestorsListOfMaps);	
		});
	}

	public List<SequenceResult> initSequenceResults(CommandContext cmdContext,
			Boolean headerDetect, Optional<String> alignmentName,
			List<AbstractSequenceObject> seqObjects) {
		List<SequenceResult> seqResults = new ArrayList<SequenceResult>();
		final Map<String, String> almtSearchStringToAlmtName;
		if(headerDetect) {
			almtSearchStringToAlmtName = new LinkedHashMap<String, String>();
			List<String> allRefNames = cmdContext.cmdBuilder(ListAlignmentCommand.class).execute().getColumnValues(Alignment.NAME_PROPERTY);
			allRefNames.forEach(almtName -> {
				if(almtName.startsWith("AL_")) {
					almtSearchStringToAlmtName.put(almtName.replaceFirst("AL_", ""), almtName);
				}
			});
		} else {
			almtSearchStringToAlmtName = null;
		}
		seqObjects.forEach(seqObj -> {
			String header = seqObj.getHeader();
			String initialAlignmentName;
			if(headerDetect) {
				initialAlignmentName = detectAlignmentNameFromHeader(almtSearchStringToAlmtName, header);
			} else {
				initialAlignmentName = alignmentName.get();
			}
			seqResults.add(new SequenceResult(cmdContext, "submittedData", header, initialAlignmentName, seqObj));
		});
		
		initSequenceAlignmentResults(cmdContext, seqResults);
		return seqResults;
	}

	private void generateSequenceFeatureResults(CommandContext cmdContext, Map<String, AlignmentResult> almtNameToAlmtResult,
			SequenceResult seqResult, Set<String> featureRestrictions, Set<String> referenceRestrictions) {
		for(SequenceAlignmentResult sequenceAlignmentResult : seqResult.seqAlignmentResults) {
			sequenceAlignmentResult.generateSequenceAlignmentFeatureResults(cmdContext, almtNameToAlmtResult, seqResult, 
					s2cMinorityVariantFilter, featureRestrictions, referenceRestrictions);
		}
	}
	

	// by using the "translate segments" command, we can fill in seqToRefAlignedSegments for the rest of the chain.
	private void propagateAlignedSegments(CommandContext cmdContext, 
			Map<String, AlignmentResult> almtNameToAlmtResult, SequenceResult seqResult) {
		SequenceAlignmentResult currentAlmtAnalysis = seqResult.seqAlignmentResults.get(0);
		AlignmentResult currentAlmtResult = almtNameToAlmtResult.get(currentAlmtAnalysis.getAlignmentName());
		for(SequenceAlignmentResult parentAlmtAnalysis : seqResult.seqAlignmentResults.subList(1, seqResult.seqAlignmentResults.size())) {

			List<QueryAlignedSegment> seqToRefAlignedSegments = currentAlmtAnalysis.getSeqToRefAlignedSegments();

			CommandBuilder<TranslateSegmentsResult, TranslateSegmentsCommand> cmdBuilder = 
					cmdContext.cmdBuilder(TranslateSegmentsCommand.class);
			// take queryToRef1 from the sequence to reference segments
			ArrayBuilder queryToRef1Array = cmdBuilder.setArray(TranslateSegmentsCommand.QUERY_TO_REF1_SEGMENT);
			seqToRefAlignedSegments.forEach(seg -> seg.toDocument(queryToRef1Array.addObject()));

			// take ref1ToRef2 from the reference to parent aligned segments for the child alignment
			ArrayBuilder ref1ToRef2Array = cmdBuilder.setArray(TranslateSegmentsCommand.REF1_TO_REF2_SEGMENT);
			currentAlmtResult.getRefToParentAlignedSegments().forEach(seg -> seg.toDocument(ref1ToRef2Array.addObject()));
			List<QueryAlignedSegment> parentSeqToRefAlignedSegments = cmdBuilder.execute().getResultSegments();
			parentAlmtAnalysis.setSeqToRefAlignedSegments(parentSeqToRefAlignedSegments);

			currentAlmtResult = almtNameToAlmtResult.get(parentAlmtAnalysis.getAlignmentName());
			
			parentAlmtAnalysis.setReferenceLength(currentAlmtResult.getReferenceLength());
			
			parentAlmtAnalysis.setSeqToRefReferenceCoverage(
					IQueryAlignedSegment.getReferenceNtCoveragePercent(parentSeqToRefAlignedSegments, currentAlmtResult.getReferenceLength()));

			parentAlmtAnalysis.setSeqToRefQueryCoverage(
					IQueryAlignedSegment.getQueryNtCoveragePercent(parentSeqToRefAlignedSegments, seqResult.getSequenceLength()));

			
			currentAlmtAnalysis = parentAlmtAnalysis;
		} 
		
		
	}

	// Given a reference seq and a list of seqResults where this is the ref sequence of their initial alignment
	// run the aligner to produce QueryAlignedSegments aligning each seqObj with its initial reference.
	private <R extends Aligner.AlignerResult, A extends Aligner<R,A>> void initSeqToRefAlignedSegments(
			CommandContext cmdContext, List<SequenceResult> seqResults, AlignmentResult almtResult) {
		Module alignerModule = GlueDataObject.lookup(cmdContext, 
				Module.class, Module.pkMap(alignerModuleName));
		@SuppressWarnings("unchecked")
		Aligner<R,A> aligner = (Aligner<R, A>) alignerModule.getModulePlugin(cmdContext.getGluetoolsEngine());
		try(ModeCloser moduleModeCloser = cmdContext.pushCommandMode("module", alignerModuleName)) {
			@SuppressWarnings("unchecked")
			CommandBuilder<R, ? extends AlignCommand<R,A>> cmdBuilder = cmdContext.cmdBuilder(aligner.getAlignCommandClass());
			ArrayBuilder seqArrayBuilder = cmdBuilder.
				set(AlignCommand.REFERENCE_NAME	, almtResult.getReferenceName()).
				setArray(AlignCommand.SEQUENCE);
			seqResults.forEach(seqResult -> {
				ObjectBuilder seqObjBuilder = seqArrayBuilder.addObject();
				seqObjBuilder.set(AlignCommand.QUERY_ID, constructQueryID(seqResult.getSourceName(), seqResult.getSequenceID()));
				String seqNucleotides = seqResult.getSeqObj().getNucleotides(cmdContext);
				seqObjBuilder.set(AlignCommand.NUCLEOTIDES, seqNucleotides);
			});
			R alignerResult = cmdBuilder.execute();
			Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = alignerResult.getQueryIdToAlignedSegments();
			seqResults.forEach(seqResult -> {
				SequenceAlignmentResult initialSeqAlmtResult = seqResult.seqAlignmentResults.get(0);
				List<QueryAlignedSegment> seqToRefAlignedSegments = queryIdToAlignedSegments.get(constructQueryID(seqResult.getSourceName(), seqResult.getSequenceID()));
				initialSeqAlmtResult.setSeqToRefAlignedSegments(seqToRefAlignedSegments);

				initialSeqAlmtResult.setReferenceLength(almtResult.getReferenceLength());
				
				initialSeqAlmtResult.setSeqToRefReferenceCoverage(
						IQueryAlignedSegment.getReferenceNtCoveragePercent(seqToRefAlignedSegments, almtResult.getReferenceLength()));

				initialSeqAlmtResult.setSeqToRefQueryCoverage(
						IQueryAlignedSegment.getQueryNtCoveragePercent(seqToRefAlignedSegments, seqResult.getSequenceLength()));
			});
		}
	}

	private String constructQueryID(String sourceName, String sequenceID) {
		return sourceName+"."+sequenceID;
	}
	
	private String detectAlignmentNameFromHeader(Map<String, String> almtSearchStringToAlmtName, String header) {
		int bestLength = Integer.MIN_VALUE;
		String bestMatch = null;
		for(Entry<String, String> entry: almtSearchStringToAlmtName.entrySet()) {
			String searchString = entry.getKey();
			if(header.contains(searchString)) {
				String almtName = entry.getValue();
				if(bestMatch == null || searchString.length() > bestLength) {
					bestMatch = almtName;
					bestLength = searchString.length();
				}
			}
		}
		if(bestMatch != null) {
			return bestMatch;
		}
		throw new MutationFrequenciesException(MutationFrequenciesException.Code.UNABLE_TO_DETECT_ALIGNMENT_NAME, header);
	}
	
	private class AlgnmentAnalysisResult {
		ReferenceRealisedFeatureTreeResult featureTreeResult;
		List<SequenceResult> seqResults;
		public AlgnmentAnalysisResult(
				ReferenceRealisedFeatureTreeResult featureTreeResult,
				List<SequenceResult> seqResults) {
			super();
			this.featureTreeResult = featureTreeResult;
			this.seqResults = seqResults;
		}
		
		
	}
	

	public AlignmentAnalysisResult doSingleAlignmentAnalysis(
			CommandContext cmdContext, String alignmentName,
			boolean recursive, Optional<Expression> whereClause, 
			String excludeVcatName,
			String referenceName, 
			String featureName,
			boolean excludeX) {
		
		AlgnmentAnalysisResult almtAnalysisResult = doAlignmentAnalysis(
				cmdContext, alignmentName, recursive, whereClause,
				referenceName, featureName);
		
		List<AaReferenceSegment> aaReferenceSegments = almtAnalysisResult.featureTreeResult.getAaReferenceSegments();
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();

		Map<Integer, MutationFrequencySummary> indexToMutationFreqSummary = new LinkedHashMap<Integer, MutationFrequencySummary>();
		
		for(AaReferenceSegment refSeg: aaReferenceSegments) {
			for(int i = refSeg.getRefStart(); i <= refSeg.getRefEnd(); i++) {
				indexToMutationFreqSummary.put(i, new MutationFrequencySummary(refSeg.getAminoAcidsSubsequence(i, i).toString()));
			}
		}
		
		
		for(SequenceResult seqResult: almtAnalysisResult.seqResults) {
			for(SequenceAlignmentResult seqAlmtResult : seqResult.seqAlignmentResults) {
				if(seqAlmtResult.getReferenceName().equals(referenceName)) {
					SequenceFeatureResult sequenceFeatureResult = seqAlmtResult.getSequenceFeatureResult(featureName);

					// compute those AA locations where a variation is excluded from the results because it belongs to an excluded
					// variation category.
					Set<Integer> excludedVariationCategoryPositions = 
							computeExcludedVariationCategoryPositions(cmdContext, 
									referenceName, featureName, sequenceFeatureResult, excludeVcatName);

					List<ReferenceDifferenceNote> aaReferenceDifferenceNotes = sequenceFeatureResult.getAaReferenceDifferenceNotes();
					for(ReferenceDifferenceNote refDiffNote: aaReferenceDifferenceNotes) {
						CharSequence mask = refDiffNote.getMask();
						int maskIndex = 0;
						for(int i = refDiffNote.getRefStart(); i <= refDiffNote.getRefEnd(); i++) {
							MutationFrequencySummary mutFreqSummary = indexToMutationFreqSummary.get(i);
							mutFreqSummary.totalMembers = mutFreqSummary.totalMembers+1;
							if(!excludedVariationCategoryPositions.contains(i)) {
								char memberValue = mask.charAt(maskIndex);
								if(memberValue != '-') {
									mutFreqSummary.mutationMembers.compute(new String(new char[]{memberValue}), (k, v) -> v == null ? 1 : v+1);
								}
							}
							maskIndex++;
						}
					}
					break;
				}
			}
		}
		
		indexToMutationFreqSummary.forEach((index, mutFreqSummary) -> {
			int totalMembers = mutFreqSummary.totalMembers;
			String refValue = mutFreqSummary.refValue;
			mutFreqSummary.mutationMembers.forEach((mutValue, mutMembers) -> {
				if(excludeX && mutValue.equals("X")) {
					return;
				} else {
					Map<String, Object> row = new LinkedHashMap<String, Object>();
					row.put(AlignmentAnalysisResult.CODON, index);
					row.put(AlignmentAnalysisResult.REF_AMINO_ACID, refValue);
					row.put(AlignmentAnalysisResult.MUT_AMINO_ACID, mutValue);
					row.put(AlignmentAnalysisResult.TOTAL_MEMBERS, totalMembers);
					row.put(AlignmentAnalysisResult.MUTATION_MEMBERS, mutMembers);
					rowData.add(row);
				}
			});
			
		});
		return new AlignmentAnalysisCommand.AlignmentAnalysisResult(rowData);
	}

	private AlgnmentAnalysisResult doAlignmentAnalysis(
			CommandContext cmdContext, String alignmentName, boolean recursive,
			Optional<Expression> whereClause, String referenceName,
			String featureName) {
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		if(feature.isInformational()) {
			throw new MutationFrequenciesException(MutationFrequenciesException.Code.FEATURE_IS_INFORMATIONAL, featureName);
		}
		if(feature.getOrfAncestor() == null) {
			throw new MutationFrequenciesException(MutationFrequenciesException.Code.FEATURE_IS_NOT_IN_ANY_ORF, featureName);
		}
		
		
		
		// init a list of sequence results for the alignment members.
		List<SequenceResult> seqResults = 
				initSequenceResultsForSingleAlignment(cmdContext, alignmentName, recursive, whereClause);
	
		
		Map<String, AlignmentResult> almtNameToAlmtResult = new LinkedHashMap<String, AlignmentResult>();

		// alignment result for the selected reference sequence.
		AlignmentResult referenceAlmtResult = null;

		// add alignment results for all the ancestor alignments.
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		for(Alignment ancestorAlmt : alignment.getAncestors()) {
			AlignmentResult ancestorAlmtResult = new AlignmentResult(ancestorAlmt.getName());
			Alignment parent = ancestorAlmt.getParent();
			String parentName = parent == null ? null : parent.getName();
			ancestorAlmtResult.init(cmdContext, parentName);
			if(ancestorAlmt.getRefSequence().getName().equals(referenceName)) {
				referenceAlmtResult = ancestorAlmtResult;
			}
			almtNameToAlmtResult.put(ancestorAlmt.getName(), ancestorAlmtResult);
		}
		
		if(referenceAlmtResult == null) {
			throw new MutationFrequenciesException(MutationFrequenciesException.Code.REF_SEQUENCE_DOES_NOT_CONSTRAIN_ANCESTOR, alignmentName, referenceName);
		}
		
		// set up the chain of alignments for each seq result.
		initSequenceAlignmentResults(cmdContext, seqResults);

		// generate alignment results for all other alignments involved
		addAlmtResultsFromSeqResults(almtNameToAlmtResult, cmdContext, seqResults);
		AlignmentResult almtResult = almtNameToAlmtResult.get(alignmentName);

		
		ReferenceRealisedFeatureTreeResult featureTreeResult = 
				(ReferenceRealisedFeatureTreeResult) referenceAlmtResult.getReferenceFeatureTreeResult().findFeatureTree(featureName);
		
		if(featureTreeResult == null) {
			throw new MutationFrequenciesException(MutationFrequenciesException.Code.FEATURE_LOCATION_NOT_DEFINED, referenceName, featureName);
		}
		
		// we can restrict our analysis to features in this list.
		Set<String> featureRestrictions = new LinkedHashSet<String>();
		featureRestrictions.add(feature.getName());
		Feature orfAncestor = feature.getOrfAncestor();
		if(orfAncestor != null) {
			String translateDirectly = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_ORF_DESCENDENTS_DIRECTLY);
			if(translateDirectly.equals("false")) {
				featureRestrictions.add(orfAncestor.getName());
			}
		}
		
		Set<String> referenceRestrictions = new LinkedHashSet<String>();
		referenceRestrictions.add(referenceName);
		
		// for each sequence, initialise first sequence alignment result from stored constrained alignment.
		seqResults.forEach(seqResult -> {
			SequenceAlignmentResult initialSeqAlmtResult = seqResult.seqAlignmentResults.get(0);
			Map<String, String> almtMemberPkMap = AlignmentMember.pkMap(initialSeqAlmtResult.getAlignmentName(), seqResult.getSourceName(), seqResult.getSequenceID());
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, almtMemberPkMap);
			List<QueryAlignedSegment> seqToRefAlignedSegments = almtMember.getAlignedSegments().stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
			initialSeqAlmtResult.setSeqToRefAlignedSegments(seqToRefAlignedSegments);

			initialSeqAlmtResult.setReferenceLength(almtResult.getReferenceLength());
			
			initialSeqAlmtResult.setSeqToRefReferenceCoverage(
					IQueryAlignedSegment.getReferenceNtCoveragePercent(seqToRefAlignedSegments, almtResult.getReferenceLength()));

			initialSeqAlmtResult.setSeqToRefQueryCoverage(
					IQueryAlignedSegment.getQueryNtCoveragePercent(seqToRefAlignedSegments, seqResult.getSequenceLength()));
		});		

		// for each sequence fill in seqToRefAlignedSegments for the rest of the path to the root of the tree.
		seqResults.forEach(seqResult -> propagateAlignedSegments(cmdContext, almtNameToAlmtResult, seqResult));
		
		// for each sequence, and each alignment in its path, generate SequenceFeatureResults.
		seqResults.forEach(seqResult -> generateSequenceFeatureResults(cmdContext, almtNameToAlmtResult, seqResult, featureRestrictions, referenceRestrictions));

		AlgnmentAnalysisResult almtAnalysisResult = new AlgnmentAnalysisResult(featureTreeResult, seqResults);
		return almtAnalysisResult;
	}
	
	private Set<Integer> computeExcludedVariationCategoryPositions(
			CommandContext cmdContext,
			String referenceName, String featureName,
			SequenceFeatureResult sequenceFeatureResult, String excludeVcatName) {
		LinkedHashSet<Integer> excludedVariationCategoryPositions = new LinkedHashSet<Integer>();
		if(excludeVcatName != null) {
			VariationCategory excludedVcat = GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(excludeVcatName));
			List<VariationNote> aaVariationNotes = sequenceFeatureResult.getAaVariationNotes();
			for(VariationNote aaVariationNote: aaVariationNotes) {
				Variation variation = GlueDataObject.lookup(cmdContext, Variation.class, 
						Variation.pkMap(referenceName, featureName, aaVariationNote.getVariationName()));
				boolean isExcluded = variation.getVcatMemberships().stream().anyMatch(vcm -> vcm.getCategory().getAncestors().contains(excludedVcat));
				if(isExcluded) {
					for(int i = aaVariationNote.getRefStart(); i <= aaVariationNote.getRefEnd(); i++) {
						excludedVariationCategoryPositions.add(i);
					}
				}
			}
			
		}
		return excludedVariationCategoryPositions;
	}

	private static class MutationFrequencySummary {
		String refValue;
		Integer totalMembers = 0;
		Map<String, Integer> mutationMembers = new LinkedHashMap<String, Integer>();
		public MutationFrequencySummary(String refValue) {
			super();
			this.refValue = refValue;
		}

	}
	

	private List<SequenceResult> initSequenceResultsForSingleAlignment(
			CommandContext cmdContext, String alignmentName, boolean recursive, Optional<Expression> whereClause) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		if(alignment.getRefSequence() == null) {
			throw new MutationFrequenciesException(MutationFrequenciesException.Code.ALIGNMENT_IS_UNCONSTRAINED, alignmentName);
		}
		
		List<Alignment> alignments = new ArrayList<Alignment>(Collections.singletonList(alignment));
		if(recursive) {
			alignments.addAll(alignment.getDescendents());
		} 

		Expression exp = ExpressionFactory.expFalse();
		for(Alignment startingAlignment: alignments) {
			exp = exp.orExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, startingAlignment.getName()));
		}
		if(whereClause.isPresent()) {
			exp = exp.andExp(whereClause.get());
		}
		List<AlignmentMember> members = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, exp));
		List<SequenceResult> seqResults = members.stream().map(almtMember -> 
			{ 
				Sequence sequence = almtMember.getSequence();
				SequenceResult seqResult = new SequenceResult(cmdContext, 
						sequence.getSource().getName(), sequence.getSequenceID(), 
						almtMember.getAlignment().getName(), sequence.getSequenceObject());
				
				return seqResult;
				
			}).collect(Collectors.toList());
		return seqResults;
	}

	public PreviewVariationsResult previewVariations(
			CommandContext cmdContext, String alignmentName, boolean recursive,
			Optional<Expression> whereClause, String referenceName,
			String featureName) {
		AlignmentAnalysisResult analysisResult = doSingleAlignmentAnalysis(cmdContext, alignmentName, recursive, whereClause, null, referenceName, featureName, true);
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		analysisResult.asListOfMaps().forEach(analysisRow -> {
			Integer mutationMembers = (Integer) analysisRow.get(AlignmentAnalysisResult.MUTATION_MEMBERS);
			Integer totalMembers = (Integer) analysisRow.get(AlignmentAnalysisResult.TOTAL_MEMBERS);
			if( ( (double) mutationMembers / (double) totalMembers ) * 100.0 < generatedVariationPercentThreshold ) {
				return;
			}
			TemplateHashModel variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					Object object = analysisRow.get(key);
					return object == null ? null : new SimpleScalar(object.toString());
				}
				@Override
				public boolean isEmpty() { return false; }
			};
			StringWriter stringWriter = new StringWriter();
			try {
				generatedVariationNameTemplate.process(variableResolver, stringWriter);
			} catch (TemplateException te) {
				throw new MutationFrequenciesException(te, 
						MutationFrequenciesException.Code.VARIATION_NAME_TEMPLATE_FAILED, te.getLocalizedMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put(PreviewVariationsResult.VARIATION_NAME, stringWriter.toString());
			row.put(PreviewVariationsResult.REF_START, analysisRow.get(AlignmentAnalysisResult.CODON));
			row.put(PreviewVariationsResult.REF_END, analysisRow.get(AlignmentAnalysisResult.CODON));
			row.put(PreviewVariationsResult.REGEX, "["+
					( (String) analysisRow.get(AlignmentAnalysisResult.MUT_AMINO_ACID) ) + "]");
			row.put(PreviewVariationsResult.TRANSLATION_FORMAT, TranslationFormat.AMINO_ACID.name());
			rowData.add(row);
		});
		return new PreviewVariationsResult(rowData);
	}

	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this reporter") 
	public static class ShowMutationFrequenciesReporterCommand extends ShowConfigCommand<MutationFrequenciesReporter> {}

	
	@SimpleConfigureCommandClass(
			propertyNames={ALIGNER_MODULE_NAME, GENERATED_VARIATION_NAME_TEMPLATE, GENERATED_VARIATION_PERCENT_THRESHOLD}
	)
	public static class ConfigureMutationFrequenciesReporterCommand extends SimpleConfigureCommand<MutationFrequenciesReporter> {}


	public boolean mergeGeneratedVariations() {
		return mergeGeneratedVariations;
	}

	public AlignmentVariationScanResult doAlignmentVariationScan(
			CommandContext cmdContext, String alignmentName, boolean recursive,
			Optional<Expression> whereClause, String referenceName,
			String featureName, String variationName) {

		AlgnmentAnalysisResult alignmentAnalysisResult = doAlignmentAnalysis(cmdContext, alignmentName, recursive, whereClause, referenceName, featureName);
	
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		
		alignmentAnalysisResult.seqResults.forEach(seqResult -> {
			seqResult.getSeqAlignmentResults().forEach(seqAlignmentResult -> {
				if(seqAlignmentResult.getReferenceName().equals(referenceName)) {
					SequenceFeatureResult sequenceFeatureResult = seqAlignmentResult.getSequenceFeatureResult(featureName);
					if(sequenceFeatureResult != null) {
						List<VariationNote> aaVariationNotes = sequenceFeatureResult.getAaVariationNotes();
						if(aaVariationNotes != null) {
							for(VariationNote aaVariationNote: aaVariationNotes) {
								if(aaVariationNote.getVariationName().equals(variationName)) {
									Map<String, Object> row = new LinkedHashMap<String, Object>();
									row.put(AlignmentVariationScanResult.ALIGNMENT_NAME, seqResult.getInitialAlignmentName());
									row.put(AlignmentVariationScanResult.MEMBER_SOURCE, seqResult.getSourceName());
									row.put(AlignmentVariationScanResult.MEMBER_SEQUENCE_ID, seqResult.getSequenceID());
									rowData.add(row);
								}
							}
						}
					}
				}
			});
		});
		
		
		return new AlignmentVariationScanResult(rowData);
	
	}

	
	
	
}
