package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ListAlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand.TranslateSegmentsResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentShowAncestorsCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusMinorityVariantFilter;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.TransientAnalysisCommand.TransientAnalysisResult;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="mutationFrequenciesReporter")
public class MutationFrequenciesReporter extends ModulePlugin<MutationFrequenciesReporter> {

	private String alignerModuleName;
	private Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter;
	
	public MutationFrequenciesReporter() {
		addProvidedCmdClass(TransientAnalysisCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		alignerModuleName = PluginUtils.configureStringProperty(configElem, "alignerModuleName", true);
		s2cMinorityVariantFilter = new Sam2ConsensusMinorityVariantFilter();
		Element s2cMinorityVariantFilterElem = PluginUtils.findConfigElement(configElem, "sam2ConsensusMinorityVariantFilter");
		if(s2cMinorityVariantFilterElem != null) {
			s2cMinorityVariantFilter.configure(pluginConfigContext, s2cMinorityVariantFilterElem);
		}
		
	}

	public TransientAnalysisResult doTransientAnalysis(CommandContext cmdContext,
			Boolean headerDetect, Optional<String> alignmentName, byte[] sequenceData) {

		List<AbstractSequenceObject> seqObjects = FastaUtils.seqObjectsFromSeqData(sequenceData);
		
		// initialize a sequence result for each sequence object, including
		// selecting an initial alignment
		List<SequenceResult> seqResults = initSequenceResults(cmdContext, headerDetect, alignmentName, seqObjects);
		
		// generate alignment results for all alignments involved
		Map<String, AlignmentResult> almtNameToAlmtResult = generateAlignmentResults(cmdContext, seqResults);
		
		// generate the initial segments aligning each sequence to the selected reference.
		generateInitialAlignments(cmdContext, seqResults, almtNameToAlmtResult);
		
		// for each sequence fill in seqToRefAlignedSegments for the rest of the path to the root of the tree.
		seqResults.forEach(seqResult -> propagateAlignedSegments(cmdContext, almtNameToAlmtResult, seqResult));
		
		// for each sequence, and each alignment in its path, generate SequenceFeatureResults.
		seqResults.forEach(seqResult -> generateSequenceFeatureResults(cmdContext, almtNameToAlmtResult, seqResult));
		
		return new TransientAnalysisResult(new ArrayList<AlignmentResult>(almtNameToAlmtResult.values()), seqResults);
	}

	public void generateInitialAlignments(CommandContext cmdContext,
			List<SequenceResult> seqResults,
			Map<String, AlignmentResult> almtNameToAlmtResult) {
		// group sequence results by the name of the initial alignment.
		Map<String, List<SequenceResult>> refNameToSeqResults = 
				seqResults.stream().collect(Collectors.groupingBy(seqResult -> 
					seqResult.getInitialAlignmentName()));
		
		// for each list of sequences which have the same starting alignment, align them to the reference of that alignment.
		// this produces seqToRefAlignedSegments for the first alignment analysis object in the chain.
		refNameToSeqResults.forEach((almtName, refSeqResults) -> {
			initSeqToRefAlignedSegments(cmdContext, refSeqResults, almtNameToAlmtResult.get(almtName));
		});
	}

	public Map<String, AlignmentResult> generateAlignmentResults(
			CommandContext cmdContext, List<SequenceResult> seqResults) {
		// collect together all alignments which will be involved, mapping their names to the relevant alignmentResults
		Map<String, AlignmentResult> almtNameToAlmtResult = new LinkedHashMap<String, AlignmentResult>();
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
		return almtNameToAlmtResult;
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

	private void generateSequenceFeatureResults(CommandContext cmdContext, Map<String, AlignmentResult> almtNameToAlmtResult, SequenceResult seqResult) {
		for(SequenceAlignmentResult sequenceAlignmentResult : seqResult.seqAlignmentResults) {
			sequenceAlignmentResult.generateSequenceAlignmentFeatureResults(cmdContext, almtNameToAlmtResult, seqResult, 
					s2cMinorityVariantFilter);
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
		Module alignerModule = GlueDataObject.lookup(cmdContext.getObjectContext(), 
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
	
	
	
}
