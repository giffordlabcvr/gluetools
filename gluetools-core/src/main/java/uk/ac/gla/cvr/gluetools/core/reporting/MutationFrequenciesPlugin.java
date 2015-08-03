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
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.project.AlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.ReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.SequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.MemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.ShowReferenceSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.ListAlignedSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.FeatureCommand;
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
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.SegmentUtils.Segment;

@PluginClass(elemName="mutationFrequencies")
public class MutationFrequenciesPlugin extends ModulePlugin<MutationFrequenciesPlugin> {

	private String alignmentName;
	
	public MutationFrequenciesPlugin() {
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
		Element almtCmdElem = CommandUsage.docElemForCmdClass(AlignmentCommand.class);
		GlueXmlUtils.appendElementWithText(almtCmdElem, AlignmentCommand.ALIGNMENT_NAME, alignmentName);
		cmdContext.executeElem(almtCmdElem.getOwnerDocument().getDocumentElement());
		try {
			Element showRefElement = CommandUsage.docElemForCmdClass(ShowReferenceSequenceCommand.class);
			ShowReferenceResult showReferenceResult = (ShowReferenceResult) cmdContext.
					executeElem(showRefElement.getOwnerDocument().getDocumentElement());
			refSeqName = showReferenceResult.getReferenceName();
		} finally {
			cmdContext.popCommandMode();
		}
		// go into reference sequence and find feature segments.
		Element refSeqElem = CommandUsage.docElemForCmdClass(ReferenceSequenceCommand.class);
		GlueXmlUtils.appendElementWithText(refSeqElem, ReferenceSequenceCommand.REF_SEQ_NAME, refSeqName);
		cmdContext.executeElem(refSeqElem.getOwnerDocument().getDocumentElement());
		try {
			Element showSeqElement = CommandUsage.docElemForCmdClass(ShowSequenceCommand.class);
			ShowSequenceResult showSequenceResult = (ShowSequenceResult) cmdContext.
					executeElem(showSeqElement.getOwnerDocument().getDocumentElement());
			analysisData.refSeqSourceName = showSequenceResult.getSourceName();
			analysisData.refSeqId = showSequenceResult.getSequenceID();
			Element featureElem = CommandUsage.docElemForCmdClass(FeatureCommand.class);
			GlueXmlUtils.appendElementWithText(featureElem, FeatureCommand.FEATURE_NAME, featureName);
			cmdContext.executeElem(featureElem.getOwnerDocument().getDocumentElement());
			try {
				Element listFeatSegElem = CommandUsage.docElemForCmdClass(ListFeatureSegmentCommand.class);
				ListResult listFeatResult = (ListResult) cmdContext.
						executeElem(listFeatSegElem.getOwnerDocument().getDocumentElement());
				listFeatResult.asListOfMaps().forEach(featSeg -> {
					int refStart = Integer.parseInt(featSeg.get(FeatureSegment.REF_START_PROPERTY));
					int refEnd = Integer.parseInt(featSeg.get(FeatureSegment.REF_END_PROPERTY));
					analysisData.referenceSegments.add(new ReferenceSegment(refStart, refEnd));
				});
			} finally {
				cmdContext.popCommandMode();
			}
		} finally {
			cmdContext.popCommandMode();
		}
		// go into reference sequence sequence and find segment nucleotides.
		
		String seqSourceName = analysisData.refSeqSourceName;
		String seqSeqId = analysisData.refSeqId;
		
		populateSegmentNTs(cmdContext, analysisData.referenceSegments, seqSourceName, seqSeqId, new Function<ReferenceSegment, Integer>(){
			@Override
			public Integer apply(ReferenceSegment t) {
				return t.start;
			}
			
		}, 
		new Function<ReferenceSegment, Integer>(){

			@Override
			public Integer apply(ReferenceSegment t) {
				return t.end;
			}
			
		});

		
	}

	private <T extends ReferenceSegment> void populateSegmentNTs(CommandContext cmdContext,
			List<T> segments, String seqSourceName,
			String seqSeqId,
			Function<T, Integer> getStart, Function<T, Integer> getEnd) {
		Element sequenceElem = CommandUsage.docElemForCmdClass(SequenceCommand.class);
		GlueXmlUtils.appendElementWithText(sequenceElem, SequenceCommand.SOURCE_NAME, seqSourceName);
		GlueXmlUtils.appendElementWithText(sequenceElem, SequenceCommand.SEQUENCE_ID, seqSeqId);
		cmdContext.executeElem(sequenceElem.getOwnerDocument().getDocumentElement());
		try {
			for(T segment : segments) {
				Element showNtElem = CommandUsage.docElemForCmdClass(ShowNucleotidesCommand.class);
				GlueXmlUtils.appendElementWithText(showNtElem, ShowNucleotidesCommand.BEGIN_INDEX, Integer.toString(getStart.apply(segment)));
				GlueXmlUtils.appendElementWithText(showNtElem, ShowNucleotidesCommand.END_INDEX, Integer.toString(getEnd.apply(segment)));
				NucleotidesResult ntResult = (NucleotidesResult) cmdContext.
						executeElem(showNtElem.getOwnerDocument().getDocumentElement());
				segment.nucleotides = ntResult.getNucleotides();
			}
		} finally {
			cmdContext.popCommandMode();
		}
	}

	public CommandResult doGenerate(CommandContext cmdContext,
			Optional<String> taxon, String feature) {
		
		AnalysisData analysisData = new AnalysisData();
		
		getReferenceSeqData(cmdContext, feature, analysisData);
		
		getMemberDatas(cmdContext, taxon, analysisData);
		
		
		Element rootElem = GlueXmlUtils.documentWithElement("mutationSet");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		for(ReferenceSegment refSeg : analysisData.referenceSegments) {
			int aaStartIndex = refSeg.start;
			while(aaStartIndex+2 <= refSeg.end) {
				String referenceAaString = getCodonAtNtPosition(refSeg, aaStartIndex, false);
				Element aaLocusElem = GlueXmlUtils.appendElement(rootElem, "aaLocus");
				JsonUtils.setJsonType(aaLocusElem, JsonType.Object, true);
				GlueXmlUtils.appendElementWithText(aaLocusElem, "consensusAA", referenceAaString, JsonType.String);
				
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
				GlueXmlUtils.appendElementWithText(aaLocusElem, "numIsolates", Integer.toString(numIsolates), JsonType.Integer);

				List<Entry<String, Integer>> sortedMutations = mutationAaToNumIsolates.entrySet().stream().sorted((o1, o2) -> 
					(0 - Integer.compare(o1.getValue(), o2.getValue()))).collect(Collectors.toList());
				
				sortedMutations.forEach(mut -> {
					String mutationAA = mut.getKey();
					Integer numMutIsolates = mut.getValue();
					double mutPercentage = 100 * ( numMutIsolates / (double) numIsolates ) ;
					if(mutPercentage > 1.0) {
						Element mutationElem = GlueXmlUtils.appendElement(aaLocusElem, "mutation");
						JsonUtils.setJsonType(mutationElem, JsonType.Object, true);
						GlueXmlUtils.appendElementWithText(mutationElem, "mutationAA", mutationAA, JsonType.String);
						GlueXmlUtils.appendElementWithText(mutationElem, "isolatesPercent", Double.toString(mutPercentage), JsonType.Double);
					}
				});
				aaStartIndex += 3;
			}
		}
		return new CommandResult(rootElem.getOwnerDocument());
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
			aaString = new DNASequence(codon).getRNASequence().getProteinSequence().getSequenceAsString();
		} catch (CompoundNotFoundException e) {
			throw new RuntimeException(e);
		}
		return aaString;
	}

	private void getMemberDatas(CommandContext cmdContext,
			Optional<String> taxon, AnalysisData analysisData) {
		Element almtCmdElem = CommandUsage.docElemForCmdClass(AlignmentCommand.class);
		GlueXmlUtils.appendElementWithText(almtCmdElem, AlignmentCommand.ALIGNMENT_NAME, alignmentName);
		cmdContext.executeElem(almtCmdElem.getOwnerDocument().getDocumentElement());
		try {
			Element listAlmtMembElem = CommandUsage.docElemForCmdClass(ListMemberCommand.class);
			if(taxon.isPresent() && !taxon.equals("all")) {
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
				GlueXmlUtils.appendElementWithText(listAlmtMembElem, ListMemberCommand.WHERE_CLAUSE, whereClauseExpression.toString());
			}

			ListResult listAlmtMembResult = (ListResult) cmdContext.executeElem(listAlmtMembElem.getOwnerDocument().getDocumentElement());
			List<Map<String, String>> membIdMaps = listAlmtMembResult.asListOfMaps();
			// enter each member and get overlapping segment coordinates.
			for(Map<String, String> membIdMap: membIdMaps) {
				String memberSourceName = membIdMap.get(Sequence.SOURCE_NAME_PATH);
				String memberSequenceId = membIdMap.get(Sequence.SEQUENCE_ID_PROPERTY);
				MemberData memberData = new MemberData(memberSourceName, memberSequenceId);
				analysisData.memberDatas.add(memberData);
				Element memberElem = CommandUsage.docElemForCmdClass(MemberCommand.class);
				GlueXmlUtils.appendElementWithText(memberElem, MemberCommand.SOURCE_NAME, memberSourceName);
				GlueXmlUtils.appendElementWithText(memberElem, MemberCommand.SEQUENCE_ID, memberSequenceId);
				cmdContext.executeElem(memberElem.getOwnerDocument().getDocumentElement());
				try {
					
					Element listAlignedSegmentElem = CommandUsage.docElemForCmdClass(ListAlignedSegmentCommand.class);
					ListResult listAlignedSegResult = (ListResult) cmdContext.
							executeElem(listAlignedSegmentElem.getOwnerDocument().getDocumentElement());
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
					
				} finally {
					cmdContext.popCommandMode();
				}			
				
				
			}
		} finally {
			cmdContext.popCommandMode();
		}

		// now enter each member sequence and populate segment nucleotides.
		for(MemberData memberData: analysisData.memberDatas) {
			populateSegmentNTs(cmdContext, memberData.memberSegments, memberData.sourceName, memberData.sequenceID, 
			new Function<MemberSegment, Integer>(){
				@Override
				public Integer apply(MemberSegment t) {
					return t.memberStart;
				}
				
			}, 
			new Function<MemberSegment, Integer>(){

				@Override
				public Integer apply(MemberSegment t) {
					return t.memberEnd;
				}
				
			});
		}
	}

}
