package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
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
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.transcription.TranscriptionEngine;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.ListAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ShowSequenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature.ListFeatureSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.NucleotidesResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowNucleotidesCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.SegmentUtils.Segment;

@PluginClass(elemName="mutationFrequencies")
public class MutationFrequenciesPlugin extends ModulePlugin<MutationFrequenciesPlugin> {

	private String alignmentName;
	private TranscriptionEngine transcriptionEngine;
	
	public MutationFrequenciesPlugin() {
		transcriptionEngine = new TranscriptionEngine.Builder().initMet(false).build();
		addProvidedCmdClass(GenerateCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	@CommandClass(
			commandWords="generate", 
			description = "Analyse mutations for a given taxon/feature", 
			docoptUsages = { "[-t <taxon>] -f <feature>" }, 
			docoptOptions = {
					"-t <taxon>, --taxon <taxon>        Restrict analysis by taxon",
					"-f <feature>, --feature <feature>  Specify genome feature"
			}
	)
	public static class GenerateCommand extends ModuleProvidedCommand<MutationFrequenciesPlugin> implements ProvidedProjectModeCommand {
		
		private Optional<String> taxon;
		private String feature;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext,
				Element configElem) {
			super.configure(pluginConfigContext, configElem);
			taxon = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "taxon", false));
			feature = PluginUtils.configureStringProperty(configElem, "feature", true);
		}

		@Override
		protected CommandResult execute(CommandContext cmdContext, MutationFrequenciesPlugin mutationFrequenciesPlugin) {
			return mutationFrequenciesPlugin.doGenerate(cmdContext, taxon, feature);
		}

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
			ShowReferenceResult showReferenceResult = 
					(ShowReferenceResult) cmdContext.cmdBuilder(ShowReferenceSequenceCommand.class).execute();
			refSeqName = showReferenceResult.getReferenceName();
		}
		// go into reference sequence and find feature segments.
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", refSeqName)) {
			ShowSequenceResult showSequenceResult = 
					(ShowSequenceResult) cmdContext.cmdBuilder(ShowSequenceCommand.class).execute();
			analysisData.refSeqSourceName = showSequenceResult.getSourceName();
			analysisData.refSeqId = showSequenceResult.getSequenceID();
			try (ModeCloser featureMode = cmdContext.pushCommandMode("feature", featureName)) {
				ListResult listFeatResult = (ListResult) cmdContext.
						cmdBuilder(ListFeatureSegmentCommand.class).execute();
				listFeatResult.asListOfMaps().forEach(featSeg -> {
					int refStart = Integer.parseInt(featSeg.get(FeatureSegment.REF_START_PROPERTY));
					int refEnd = Integer.parseInt(featSeg.get(FeatureSegment.REF_END_PROPERTY));
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
			NucleotidesResult ntResult = (NucleotidesResult) cmdContext.cmdBuilder(ShowNucleotidesCommand.class).execute();
			for(T segment : segments) {
				segment.nucleotides = SegmentUtils.subSeq(ntResult.getNucleotides(), getStart.apply(segment), getEnd.apply(segment));
			}
		}
	}

	public CommandResult doGenerate(CommandContext cmdContext,
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
			CommandBuilder<ListMemberCommand> listMemberBuilder = cmdContext.cmdBuilder(ListMemberCommand.class);
			if(taxon.isPresent() && !taxon.get().equals("all")) {
				String taxonString = taxon.get();
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
				Expression whereClauseExpression = ExpressionFactory.matchExp("GENOTYPE", genotype);
				if(subtype.trim().length() != 0) {
					whereClauseExpression = whereClauseExpression.andExp(ExpressionFactory.matchExp("SUBTYPE", subtype));
				}
				listMemberBuilder.set(ListMemberCommand.WHERE_CLAUSE, whereClauseExpression.toString());
			}
			// System.out.println("P1a:"+(System.currentTimeMillis()-start) % 10000);

			ListResult listAlmtMembResult = (ListResult) listMemberBuilder.execute();
			List<Map<String, String>> membIdMaps = listAlmtMembResult.asListOfMaps();
			// enter each member and get overlapping segment coordinates.
			for(Map<String, String> membIdMap: membIdMaps) {
				String memberSourceName = membIdMap.get(Sequence.SOURCE_NAME_PATH);
				String memberSequenceId = membIdMap.get(Sequence.SEQUENCE_ID_PROPERTY);
				MemberData memberData = new MemberData(memberSourceName, memberSequenceId);
				analysisData.memberDatas.add(memberData);
				try (ModeCloser memberMode = 
						cmdContext.pushCommandMode("member", memberSourceName, memberSequenceId)) {
					ListResult listAlignedSegResult = (ListResult) cmdContext.
							cmdBuilder(ListAlignedSegmentCommand.class).execute();
					listAlignedSegResult.asListOfMaps().forEach(alignedSeg -> {
						int alnRefStart = Integer.parseInt(alignedSeg.get(AlignedSegment.REF_START_PROPERTY));
						int alnRefEnd = Integer.parseInt(alignedSeg.get(AlignedSegment.REF_END_PROPERTY));
						int alnMembStart = Integer.parseInt(alignedSeg.get(AlignedSegment.MEMBER_START_PROPERTY));
						int alnMembEnd = Integer.parseInt(alignedSeg.get(AlignedSegment.MEMBER_END_PROPERTY));
						
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
	
}
