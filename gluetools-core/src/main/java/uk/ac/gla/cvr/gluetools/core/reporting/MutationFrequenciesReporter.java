package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.transcription.TranscriptionEngine;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.ListAlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.TranslateSegmentsCommand.TranslateSegmentsResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentShowAncestorsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.ListAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.ListAlignedSegmentCommand.ListAlignedSegmentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand.ReferenceShowSequenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.ListFeatureSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.NucleotidesResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceShowLengthCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowNucleotidesCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.FastaSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils.Segment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="mutationFrequenciesReporter")
public class MutationFrequenciesReporter extends ModulePlugin<MutationFrequenciesReporter> {

	private String alignmentName;
	private String alignerModuleName;
	private TranscriptionEngine transcriptionEngine;
	
	public MutationFrequenciesReporter() {
		transcriptionEngine = new TranscriptionEngine.Builder().initMet(false).build();
		addProvidedCmdClass(GenerateCommand.class);
		addProvidedCmdClass(TransientAnalysisCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
		alignerModuleName = PluginUtils.configureStringProperty(configElem, "alignerModuleName", true);
	}

	private class ReferenceSegment extends SegmentUtils.Segment {
		public ReferenceSegment(int start, int end) {
			super(start, end);
		}
		String nucleotides;
		
		public String toString() {
			return "["+start+", "+end+"]:"+nucleotides;
		}
	}

	private class MemberSegment extends ReferenceSegment {
		int memberStart, memberEnd;
		public MemberSegment(int refStart, int refEnd, int memberStart, int memberEnd) {
			super(refStart, refEnd);
			this.memberStart = memberStart;
			this.memberEnd = memberEnd;
		}
		public String toString() {
			return "["+start+", "+end+"]:"+nucleotides;
		}
	}

	private class MemberData {
		String sourceName;
		String sequenceID;
		List<MemberSegment> memberSegments = new ArrayList<MemberSegment>();
		
		public MemberData(String sourceName, String sequenceID) {
			super();
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
		}
		
		public String toString() {
			return "Member sourceName:"+sourceName+", sequenceID:"+sequenceID+"\n"+memberSegments.toString();
		}

		
	}
	
	private class AnalysisData {
		String refSeqSourceName;
		String refSeqId;
		
		List<ReferenceSegment> referenceSegments = new ArrayList<ReferenceSegment>();
		
		List<MemberData> memberDatas = new ArrayList<MemberData>();
	}

	
	
	
	private void getReferenceSeqData(CommandContext cmdContext, String featureName,
			AnalysisData analysisData) {
		String refSeqName;
		// go into alignment and find reference sequence name
		try (ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			ShowReferenceSequenceCommand.ShowReferenceResult showReferenceResult = 
					cmdContext.cmdBuilder(ShowReferenceSequenceCommand.class).execute();
			refSeqName = showReferenceResult.getReferenceName();
		}
		// go into reference sequence and find feature-location segments.
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", refSeqName)) {
			ReferenceShowSequenceResult showSequenceResult = 
					(ReferenceShowSequenceResult) cmdContext.cmdBuilder(ReferenceShowSequenceCommand.class).execute();
			analysisData.refSeqSourceName = showSequenceResult.getSourceName();
			analysisData.refSeqId = showSequenceResult.getSequenceID();
			try (ModeCloser featureMode = cmdContext.pushCommandMode("feature-location", featureName)) {
				ListResult listFeatResult = (ListResult) cmdContext.
						cmdBuilder(ListFeatureSegmentCommand.class).execute();
				listFeatResult.asListOfMaps().forEach(featSeg -> {
					int refStart = (Integer) featSeg.get(FeatureSegment.REF_START_PROPERTY);
					int refEnd = (Integer) featSeg.get(FeatureSegment.REF_END_PROPERTY);
					analysisData.referenceSegments.add(new ReferenceSegment(refStart, refEnd));
				});
			}
		}
		// go into reference sequence sequence and find segment nucleotides.
		populateSegmentNTs(cmdContext, 
				analysisData.referenceSegments, analysisData.refSeqSourceName, analysisData.refSeqId, 
				t -> t.start, t -> t.end);

		
	}

	private <T extends ReferenceSegment> void populateSegmentNTs(CommandContext cmdContext,
			List<T> segments, String seqSourceName, String seqSeqId,
			Function<T, Integer> getStart, Function<T, Integer> getEnd) {
		try (ModeCloser seqMode = cmdContext.pushCommandMode("sequence", seqSourceName, seqSeqId)) {
			NucleotidesResult ntResult = cmdContext.cmdBuilder(ShowNucleotidesCommand.class).execute();
			for(T segment : segments) {
				segment.nucleotides = SegmentUtils.subSeq(ntResult.getNucleotides(), getStart.apply(segment), getEnd.apply(segment));
			}
		}
	}

	public MutationFrequenciesResult doGenerate(CommandContext cmdContext,
			Optional<String> taxon, String feature) {
		// long start = System.currentTimeMillis();
		
		// System.out.println("Start");
		
		AnalysisData analysisData = new AnalysisData();
		
		
		getReferenceSeqData(cmdContext, feature, analysisData);

		// System.out.println("P1:"+(System.currentTimeMillis()-start) % 10000);

		
		getMemberDatas(cmdContext, taxon, analysisData);
		
		// System.out.println("P2:"+(System.currentTimeMillis()-start) % 10000);
		
		// System.out.println("P3:"+(System.currentTimeMillis()-start) % 10000);
		return new MutationFrequenciesResult(analysisData);
	}

	private String getCodonAtNtPosition(ReferenceSegment refSeg, int aaStartIndex, boolean allowAmbiguity) {
		if(aaStartIndex < refSeg.start) {
			return null;
		}
		if(aaStartIndex+2 > refSeg.end) {
			return null;
		}
		String codon = SegmentUtils.subSeq(refSeg.nucleotides, (aaStartIndex-refSeg.start)+1, (aaStartIndex-refSeg.start)+3);
		if(!codon.matches("[ACTG]{3}")) {
			if(allowAmbiguity) {
				return null;
			} else {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Reference sequence NT ambiguity at ["+aaStartIndex+", "+(aaStartIndex+2)+"]");
			}
		}
		String aaString;
		try {
			// appying the genbank rule which means T represents Uracil for RNA.
			RNASequence rnaSequence = new RNASequence(codon.replaceAll("T", "U"));
			aaString = rnaSequence.getProteinSequence(transcriptionEngine).getSequenceAsString();
		} catch (CompoundNotFoundException e) {
			throw new RuntimeException(e);
		}
		return aaString;
	}

	private void getMemberDatas(CommandContext cmdContext,
			Optional<String> taxon, AnalysisData analysisData) {
		
		// long start = System.currentTimeMillis();
		// System.out.println("P1 Start");
		
		try (ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			CommandBuilder<ListResult, ListMemberCommand> listMemberBuilder = cmdContext.cmdBuilder(ListMemberCommand.class);
			if(taxon.isPresent() && !taxon.get().equals("all")) {
				Expression whereClauseExpression = whereClauseFromTaxon(taxon.get());
				listMemberBuilder.set(ListMemberCommand.WHERE_CLAUSE, whereClauseExpression.toString());
			}
			// System.out.println("P1a:"+(System.currentTimeMillis()-start) % 10000);

			ListResult listAlmtMembResult = listMemberBuilder.execute();
			List<Map<String, Object>> membIdMaps = listAlmtMembResult.asListOfMaps();
			// enter each member and get overlapping segment coordinates.
			for(Map<String, Object> membIdMap: membIdMaps) {
				String memberSourceName = (String) membIdMap.get(AlignmentMember.SOURCE_NAME_PATH);
				String memberSequenceId = (String) membIdMap.get(AlignmentMember.SEQUENCE_ID_PATH);
				MemberData memberData = new MemberData(memberSourceName, memberSequenceId);
				analysisData.memberDatas.add(memberData);
				try (ModeCloser memberMode = cmdContext.pushCommandMode("member", memberSourceName, memberSequenceId)) {
					ListAlignedSegmentResult listAlignedSegResult = cmdContext.cmdBuilder(ListAlignedSegmentCommand.class).execute();
					listAlignedSegResult.asQueryAlignedSegments().forEach(alignedSeg -> {
						int alnRefStart = alignedSeg.getRefStart();
						int alnRefEnd = alignedSeg.getRefEnd();
						int alnMembStart = alignedSeg.getQueryStart();
						int alnMembEnd = alignedSeg.getQueryEnd();
						
						for(ReferenceSegment refSeg: analysisData.referenceSegments) {
							// find overlap with ref segment and aligned segment in ref coordinates.
							Segment overlapRef = SegmentUtils.overlap(refSeg, new Segment(alnRefStart, alnRefEnd));
							if(overlapRef != null) {
								// translate these to member coords.
								int overlapMembStart, overlapMembEnd;

								// number of NTs to truncate from the start
								int startTruncation = overlapRef.start - alnRefStart;
								// number of NTs to truncate from the end
								int endTruncation = alnRefEnd - overlapRef.end;

								if(alnMembStart <= alnMembEnd) {
									// forward direction aligned segment
									overlapMembStart = alnMembStart+startTruncation;
									overlapMembEnd = alnMembEnd-endTruncation;
								} else {
									// reverse direction aligned segment
									overlapMembStart = alnMembStart-startTruncation;
									overlapMembEnd = alnMembEnd+endTruncation;
								}

								memberData.memberSegments.add(new MemberSegment(overlapRef.start, overlapRef.end, 
										overlapMembStart, overlapMembEnd));
							}
						}
					});
				} 			
			}
		} 
		// System.out.println("P1b:"+(System.currentTimeMillis()-start) % 10000);
		// now enter each member sequence and populate segment nucleotides.
		for(MemberData memberData: analysisData.memberDatas) {
			populateSegmentNTs(cmdContext, 
					memberData.memberSegments, memberData.sourceName, memberData.sequenceID, 
					t -> t.memberStart, t -> t.memberEnd);
		}

		// System.out.println("P1c:"+(System.currentTimeMillis()-start) % 10000);

	}

	public Expression whereClauseFromTaxon(String taxonString) {
		Pattern pattern = Pattern.compile("(\\d)([a-z]*)");
		Matcher matcher = pattern.matcher(taxonString);
		if(!matcher.find()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Invalid taxon: "+taxonString);
		}
		String genotype = matcher.group(1);
		String subtype = "";
		if(matcher.groupCount() > 1) {
			subtype = matcher.group(2);
		}
		Expression whereClauseExpression = ExpressionFactory.matchExp(AlignmentMember.SEQUENCE_PROPERTY+".GENOTYPE", genotype);
		if(subtype.trim().length() != 0) {
			whereClauseExpression = whereClauseExpression.andExp(ExpressionFactory.matchExp(AlignmentMember.SEQUENCE_PROPERTY+".SUBTYPE", subtype));
		}
		return whereClauseExpression;
	}

	public class MutationFrequenciesResult extends CommandResult {

		protected MutationFrequenciesResult(AnalysisData analysisData) {
			super("mutationSet");
			ArrayBuilder aaLocusArrayBuilder = getDocumentBuilder().setArray("aaLocus");
			for(ReferenceSegment refSeg : analysisData.referenceSegments) {
				int aaStartIndex = refSeg.start;
				while(aaStartIndex+2 <= refSeg.end) {
					String referenceAaString = getCodonAtNtPosition(refSeg, aaStartIndex, false);
					int numIsolatesN = 0;
					Map<String, Integer> mutationAaToNumIsolates = new LinkedHashMap<String, Integer>();
					for(MemberData memberData : analysisData.memberDatas) {
						for(MemberSegment memberSegment: memberData.memberSegments) {
							String memberAaString = getCodonAtNtPosition(memberSegment, aaStartIndex, true);
							if(memberAaString != null) {
								numIsolatesN++;
								if(!memberAaString.equals(referenceAaString)) {
									Integer numMutations = mutationAaToNumIsolates.getOrDefault(memberAaString, 0);
									mutationAaToNumIsolates.put(memberAaString, numMutations+1);
								}
							}
						}
					}
					int numIsolates = numIsolatesN;
					ObjectBuilder aaLocusObjectBuilder = 
							aaLocusArrayBuilder.addObject()
								.setString("consensusAA", referenceAaString)
								.setInt("numIsolates", numIsolates);

					List<Entry<String, Integer>> sortedMutations = mutationAaToNumIsolates.entrySet().stream().sorted((o1, o2) -> 
						(0 - Integer.compare(o1.getValue(), o2.getValue()))).collect(Collectors.toList());
					
					ArrayBuilder mutationsBuilder = aaLocusObjectBuilder.setArray("mutation");
					sortedMutations.forEach(mut -> {
						String mutationAA = mut.getKey();
						Integer numMutIsolates = mut.getValue();
						double mutPercentage = 100 * ( numMutIsolates / (double) numIsolates ) ;
						if(mutPercentage > 1.0) {
							mutationsBuilder.addObject()
								.setString("mutationAA", mutationAA)
								.setDouble("isolatesPercent", mutPercentage);
						}
					});
					aaStartIndex += 3;
				}
			}
		}
		
	}




	public List<SequenceResult> doTransientAnalysis(CommandContext cmdContext,
			Boolean headerDetect, Optional<String> alignmentName, 
			byte[] sequenceData) {
		AnalysisContext analysisCtx = new AnalysisContext(cmdContext);
		List<AbstractSequenceObject> seqObjects = seqObjectsFromSeqData(sequenceData);
		List<SequenceResult> seqResults = new ArrayList<SequenceResult>();
		seqObjects.forEach(seqObj -> {
			String header = seqObj.getHeader();
			String initialAlignmentName;
			if(headerDetect) {
				initialAlignmentName = detectAlignmentNameFromHeader(analysisCtx, header);
			} else {
				initialAlignmentName = alignmentName.get();
			}
			seqResults.add(new SequenceResult(analysisCtx, "submittedData", header, initialAlignmentName, seqObj));
		});
		
		// initialize the alignment analysis chain for each sequence
		seqResults.forEach(seqResult -> initSeqAlmtAnalyses(seqResult));
		
		// group sequence results by the name of the reference seq of the initial alignment.
		Map<String, List<SequenceResult>> refNameToSeqResults = 
				seqResults.stream().collect(Collectors.groupingBy(seqResult -> seqResult.almtAnalysisChain.get(0).referenceName));
		
		// for each list of sequences which have the same reference, align them to that reference.
		// this produces seqToRefAlignedSegments for the first alignment analysis object in the chain.
		refNameToSeqResults.forEach((refName, refSeqResults) -> {
			initSeqToRefAlignedSegments(cmdContext, refName, refSeqResults);
		});
		// fill in seqToRefAlignedSegments for the rest of the chain.
		seqResults.forEach(seqResult -> propagateAlignedSegments(seqResult));
		
		seqResults.forEach(seqResult -> generateFeatureLocationResults(seqResult));
		return seqResults;
	}

	private void generateFeatureLocationResults(SequenceResult seqResult) {
		for(AlignmentAnalysis alignmentAnalysis : seqResult.almtAnalysisChain) {
			generateAlignmentAnalysisFeatureLocResult(alignmentAnalysis);
		}
	}
	
	private void generateAlignmentAnalysisFeatureLocResult(AlignmentAnalysis alignmentAnalysis) {
		
	}

	// by using the "translate segments" command, we can fill in seqToRefAlignedSegments for the rest of the chain.
	private void propagateAlignedSegments(SequenceResult seqResult) {
		AlignmentAnalysis currentAlmtAnalysis = seqResult.almtAnalysisChain.get(0);
		for(AlignmentAnalysis parentAlmtAnalysis : seqResult.almtAnalysisChain.subList(1, seqResult.almtAnalysisChain.size())) {
			CommandBuilder<TranslateSegmentsResult, TranslateSegmentsCommand> cmdBuilder = 
					seqResult.analysisContext.cmdContext.cmdBuilder(TranslateSegmentsCommand.class);
			ArrayBuilder queryToRef1Array = cmdBuilder.setArray(TranslateSegmentsCommand.QUERY_TO_REF1_SEGMENT);
			currentAlmtAnalysis.seqToRefAlignedSegments.forEach(seg -> seg.toDocument(queryToRef1Array.addObject()));
			ArrayBuilder ref1ToRef2Array = cmdBuilder.setArray(TranslateSegmentsCommand.REF1_TO_REF2_SEGMENT);
			currentAlmtAnalysis.refToParentAlignedSegments.forEach(seg -> seg.toDocument(ref1ToRef2Array.addObject()));
			parentAlmtAnalysis.seqToRefAlignedSegments = cmdBuilder.execute().getResultSegments();
			parentAlmtAnalysis.sequenceLength = currentAlmtAnalysis.sequenceLength;
			parentAlmtAnalysis.seqToRefQueryCoverage = 
					IQueryAlignedSegment.getQueryNtCoveragePercent(parentAlmtAnalysis.seqToRefAlignedSegments, 
							parentAlmtAnalysis.sequenceLength);
			parentAlmtAnalysis.seqToRefReferenceCoverage = 
					IQueryAlignedSegment.getReferenceNtCoveragePercent(parentAlmtAnalysis.seqToRefAlignedSegments, 
							parentAlmtAnalysis.referenceLength);
			
			currentAlmtAnalysis = parentAlmtAnalysis;
		} 
		
		
	}

	// Given a reference seq and a list of seqResults where this is the ref sequence of their initial alignment
	// run the aligner to produce QueryAlignedSegments aligning each seqObj with its initial reference.
	private <R extends Aligner.AlignerResult, A extends Aligner<R,A>> void initSeqToRefAlignedSegments(CommandContext cmdContext,
			String refName, List<SequenceResult> seqResults) {
		Module alignerModule = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				Module.class, Module.pkMap(alignerModuleName));
		@SuppressWarnings("unchecked")
		Aligner<R,A> aligner = (Aligner<R, A>) alignerModule.getModulePlugin(cmdContext.getGluetoolsEngine());
		try(ModeCloser moduleModeCloser = cmdContext.pushCommandMode("module", alignerModuleName)) {
			@SuppressWarnings("unchecked")
			CommandBuilder<R, ? extends AlignCommand<R,A>> cmdBuilder = cmdContext.cmdBuilder(aligner.getAlignCommandClass());
			ArrayBuilder seqArrayBuilder = cmdBuilder.
				set(AlignCommand.REFERENCE_NAME	, refName).
				setArray(AlignCommand.SEQUENCE);
			seqResults.forEach(seqResult -> {
				ObjectBuilder seqObjBuilder = seqArrayBuilder.addObject();
				seqObjBuilder.set(AlignCommand.QUERY_ID, constructQueryID(seqResult.sourceName, seqResult.sequenceID));
				String seqNucleotides = seqResult.seqObj.getNucleotides();
				seqResult.almtAnalysisChain.get(0).sequenceLength = seqNucleotides.length();
				seqObjBuilder.set(AlignCommand.NUCLEOTIDES, seqNucleotides);
			});
			R alignerResult = cmdBuilder.execute();
			Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = alignerResult.getQueryIdToAlignedSegments();
			seqResults.forEach(seqResult -> {
				AlignmentAnalysis initialAlmtAnalysis = seqResult.almtAnalysisChain.get(0);
				initialAlmtAnalysis.seqToRefAlignedSegments = 
						queryIdToAlignedSegments.get(constructQueryID(seqResult.sourceName, seqResult.sequenceID));
				initialAlmtAnalysis.seqToRefQueryCoverage = 
						IQueryAlignedSegment.getQueryNtCoveragePercent(initialAlmtAnalysis.seqToRefAlignedSegments, 
								initialAlmtAnalysis.sequenceLength);
				initialAlmtAnalysis.seqToRefReferenceCoverage = 
						IQueryAlignedSegment.getReferenceNtCoveragePercent(initialAlmtAnalysis.seqToRefAlignedSegments, 
								initialAlmtAnalysis.referenceLength);
			});
		}
	}

	private String constructQueryID(String sourceName, String sequenceID) {
		return sourceName+"."+sequenceID;
	}
	
	/*
	 * Set up the chain of alignment analysis objects for the sequence results.
	 * The start of this chain is the initial alignment, the chain then follows that alignment's ancestors.
	 */
	private void initSeqAlmtAnalyses(SequenceResult seqResult) {
		String currentAlmtName = seqResult.initialAlignmentName;
		List<Map<String, Object>> ancestorListOfMaps = seqResult.analysisContext.getAlignmentAncestorsResult(currentAlmtName);
		AlignmentAnalysis initialAlmtAnalysis = new AlignmentAnalysis();
		AlignmentAnalysis currentAlmtAnalysis = initialAlmtAnalysis;
		AlignmentAnalysis prevAlmtAnalysis = null;
		for(Map<String, Object> ancestorMap: ancestorListOfMaps) {
			currentAlmtAnalysis.alignmentName = (String) ancestorMap.get(Alignment.NAME_PROPERTY);
			currentAlmtAnalysis.referenceName = (String) ancestorMap.get(Alignment.REF_SEQ_NAME_PATH);
			currentAlmtAnalysis.referenceLength = seqResult.analysisContext.getRefLength(currentAlmtAnalysis.referenceName);
			seqResult.almtAnalysisChain.add(currentAlmtAnalysis);
			if(prevAlmtAnalysis != null) {
				prevAlmtAnalysis.refToParentAlignedSegments = 
						seqResult.analysisContext.getRefToParentAlignedSegments(
									prevAlmtAnalysis.alignmentName, 
									prevAlmtAnalysis.referenceName, 
									currentAlmtAnalysis.alignmentName);
			}
			prevAlmtAnalysis = currentAlmtAnalysis;
			currentAlmtAnalysis = new AlignmentAnalysis();
		}
		
	}

	// Analysis results for a single sequence.
	public static class SequenceResult {
		private AnalysisContext analysisContext;
		private String sourceName;
		private String sequenceID;
		private String initialAlignmentName;
		private AbstractSequenceObject seqObj;
		
		// this is the "analysis chain", indicating analysis of the sequence for a path of alignments
		// through the alignment tree.
		// in future there might be different starting points for different features, e.g. in the case of 
		// recombinants or segmented genomes.
		private List<AlignmentAnalysis> almtAnalysisChain = new ArrayList<AlignmentAnalysis>();
		
		public SequenceResult(AnalysisContext analysisContext, String sourceName, String sequenceID,
				String initialAlignmentName, AbstractSequenceObject seqObj) {
			super();
			this.analysisContext = analysisContext;
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
			this.initialAlignmentName = initialAlignmentName;
			this.seqObj = seqObj;
		}

		public void toDocument(ObjectBuilder seqResultObj) {
			seqResultObj.setString("sourceName", sourceName);
			seqResultObj.setString("sequenceID", sequenceID);
			seqResultObj.setString("initialAlignmentName", initialAlignmentName);
			ArrayBuilder almtChainArray = seqResultObj.setArray("alignmentAnalysis");
			for(AlignmentAnalysis almtAnalysis: almtAnalysisChain) {
				almtAnalysis.toDocument(almtChainArray.addObject());
			}
		}
		
	}

	// Analysis result for a single sequence with respect to a given alignment, specified by alignmentName.
	// seqToRefAlignedSegments gives the aligned segments which map the nucleotides of seqObj above to the
	// reference sequence of the alignment.
	// If the alignment's reference sequence defines feature locations, those mapped nucleotides of seqObj are compared
	// with those of the reference at those feature locations, and the results are given in seqFeatureLocResults
	// The alignment may have a parent. If so, refToParentAlignedSegments is non-null.
	// The aligned segments which map this alignment's reference to the parent's
	// reference are given in refToParentAlignedSegments and analysis of the sequence with respect to the 
	// parent alignment is given in parentAlignmentAnalysis.
	private static class AlignmentAnalysis {
		private String alignmentName;
		private String referenceName;
		private int referenceLength;
		private int sequenceLength;
		private List<QueryAlignedSegment> seqToRefAlignedSegments;
		private double seqToRefQueryCoverage;
		private double seqToRefReferenceCoverage;
		private List<QueryAlignedSegment> refToParentAlignedSegments;
		private FeatureAnalysisTree featureAnalysisTree;
		
		public void toDocument(ObjectBuilder seqAlmtAnalysisObj) {
			seqAlmtAnalysisObj.set("alignmentName", alignmentName);
			seqAlmtAnalysisObj.set("referenceName", referenceName);
			seqAlmtAnalysisObj.set("referenceLength", referenceLength);
			seqAlmtAnalysisObj.set("sequenceLength", sequenceLength);
			seqAlmtAnalysisObj.set("seqToRefQueryCoverage", seqToRefQueryCoverage);
			seqAlmtAnalysisObj.set("seqToRefReferenceCoverage", seqToRefReferenceCoverage);
			ArrayBuilder seqToRefAlignedSegArray = seqAlmtAnalysisObj.setArray("seqToRefAlignedSegment");
			for(QueryAlignedSegment seqToRefAlignedSegment: seqToRefAlignedSegments) {
				seqToRefAlignedSegment.toDocument(seqToRefAlignedSegArray.addObject());
			}
			if(refToParentAlignedSegments != null) {
				ArrayBuilder refToParentAlignedSegArray = seqAlmtAnalysisObj.setArray("refToParentAlignedSegments");
				for(QueryAlignedSegment refToParentAlignedSegment: refToParentAlignedSegments) {
					refToParentAlignedSegment.toDocument(refToParentAlignedSegArray.addObject());
				}
			} 
			ObjectBuilder featureTreeObj = seqAlmtAnalysisObj.setObject("featureAnalysisTree");
			featureAnalysisTree.toDocument(featureTreeObj);

		}
	}

	private static class FeatureAnalysisTree {
		private String featureName;
		private String featureDescription;
		private List<FeatureAnalysisTree> children = new ArrayList<FeatureAnalysisTree>();

		public void toDocument(ObjectBuilder featureAnalysisObj) {
			featureAnalysisObj.set("featureName", featureName);
			featureAnalysisObj.set("featureDescription", featureDescription);
			ArrayBuilder childFeatureArray = featureAnalysisObj.setArray("features");
			for(FeatureAnalysisTree childTree: children) {
				ObjectBuilder childTreeObj = childFeatureArray.addObject();
				childTree.toDocument(childTreeObj);
			}
		}
	}
	
	public List<AbstractSequenceObject> seqObjectsFromSeqData(
			byte[] sequenceData) {
		SequenceFormat format = SequenceFormat.detectFormatFromBytes(sequenceData);
		List<AbstractSequenceObject> seqObjects;
		if(format == SequenceFormat.FASTA) {
			Map<String, DNASequence> fastaMap = FastaUtils.parseFasta(sequenceData);
			seqObjects = fastaMap.entrySet().stream()
					.map(ent -> new FastaSequenceObject(ent.getKey(), ent.getValue().toString()))
					.collect(Collectors.toList());
		} else {
			AbstractSequenceObject seqObj = format.sequenceObject();
			seqObj.fromOriginalData(sequenceData);
			seqObjects = Collections.singletonList(seqObj);
		}
		return seqObjects;
	}
	

	private String detectAlignmentNameFromHeader(AnalysisContext analysisCtx, String header) {
		Map<String, String> almtSearchStringToAlmtName = analysisCtx.getAlmtSearchStringToAlmtName();
		for(Entry<String, String> entry: almtSearchStringToAlmtName.entrySet()) {
			if(header.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		throw new MutationFrequenciesException(MutationFrequenciesException.Code.UNABLE_TO_DETECT_ALIGNMENT_NAME, header);
	}

	
	private static class AnalysisContext {
		private CommandContext cmdContext;
		private Map<String, String> almtSearchStringToAlmtName;
		
		private Map<String, List<Map<String,Object>>> almtNameToAncestorsListOfMaps = new LinkedHashMap<String, List<Map<String,Object>>>();
		
		private Map<String, List<QueryAlignedSegment>> almtNameToRefParentAlignedSegments = new LinkedHashMap<String, List<QueryAlignedSegment>>();
		
		private Map<String, Integer> refNameToLength = new LinkedHashMap<String, Integer>();
		
		public AnalysisContext(CommandContext cmdContext) {
			super();
			this.cmdContext = cmdContext;
		}

		public List<QueryAlignedSegment> getRefToParentAlignedSegments(String alignmentName, String referenceName, String parentAlignmentName) {
			List<QueryAlignedSegment> refToParentAlignedSegments = almtNameToRefParentAlignedSegments.get(alignmentName);
			if(refToParentAlignedSegments == null) {
				String refSourceName;
				String refSequenceID;
				try(ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", referenceName)) {
					ReferenceShowSequenceResult refShowSeqResult = cmdContext.cmdBuilder(ReferenceShowSequenceCommand.class).execute();
					refSourceName = refShowSeqResult.getSourceName();
					refSequenceID = refShowSeqResult.getSequenceID();
				}
				try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", parentAlignmentName)) {
					try(ModeCloser memberMode = cmdContext.pushCommandMode("member", refSourceName, refSequenceID)) {
						refToParentAlignedSegments = 
								cmdContext.cmdBuilder(ListAlignedSegmentCommand.class).execute().asQueryAlignedSegments();
					}
				}
				almtNameToRefParentAlignedSegments.put(alignmentName, refToParentAlignedSegments);
			}
			return refToParentAlignedSegments;
		}

		public Map<String, String> getAlmtSearchStringToAlmtName() {
			if(almtSearchStringToAlmtName == null) {
				almtSearchStringToAlmtName = new LinkedHashMap<String, String>();
				List<String> allRefNames = cmdContext.cmdBuilder(ListAlignmentCommand.class).execute().getColumnValues(Alignment.NAME_PROPERTY);
				allRefNames.forEach(almtName -> {
					if(almtName.startsWith("AL_")) {
						almtSearchStringToAlmtName.put(almtName.replaceFirst("AL_", ""), almtName);
					}
				});
			}
			return almtSearchStringToAlmtName;
		}

		public List<Map<String,Object>> getAlignmentAncestorsResult(String alignmentName) {
			List<Map<String,Object>> ancestorsListOfMaps = almtNameToAncestorsListOfMaps.get(alignmentName);
			if(ancestorsListOfMaps == null) {
				try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
					ancestorsListOfMaps = cmdContext.cmdBuilder(AlignmentShowAncestorsCommand.class).execute().asListOfMaps();
				}
				almtNameToAncestorsListOfMaps.put(alignmentName, ancestorsListOfMaps);
			}
			return ancestorsListOfMaps;
		}
		
		public Integer getRefLength(String referenceName) {
			Integer refLength = refNameToLength.get(referenceName);
			if(refLength == null) {
				String refSourceName;
				String refSequenceID;
				try(ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", referenceName)) {
					ReferenceShowSequenceResult refShowSeqResult = cmdContext.cmdBuilder(ReferenceShowSequenceCommand.class).execute();
					refSourceName = refShowSeqResult.getSourceName();
					refSequenceID = refShowSeqResult.getSequenceID();
				}
				try(ModeCloser seqMode = cmdContext.pushCommandMode("sequence", refSourceName, refSequenceID)) {
					refLength = cmdContext.cmdBuilder(SequenceShowLengthCommand.class).execute().getLength();
				}
				refNameToLength.put(referenceName, refLength);
			}
			return refLength;
		}
		
		
	}
	
}
