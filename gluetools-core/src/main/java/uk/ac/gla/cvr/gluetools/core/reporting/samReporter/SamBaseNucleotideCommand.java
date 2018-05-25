/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public abstract class SamBaseNucleotideCommand<R extends CommandResult> extends AlignmentTreeSamReporterCommand<R> 
	implements ProvidedProjectModeCommand, 
	SamPairedParallelProcessor<SamBaseNucleotideCommand.BaseNucleotideContext, SamBaseNucleotideCommand.BaseNucleotideResult>{


	@Override
	protected final R execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		String samFileName = getFileName();

		try(SamFileSession samFileSession = SamReporterPreprocessor.preprocessSam(consoleCmdContext, samFileName, validationStringency)) {
			DNASequence consensusSequence = null;
			ReferenceSequence targetRef;
			AlignmentMember tipAlmtMember;
			if(useMaxLikelihoodPlacer()) {
				Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, samFileName, samFileSession,
						validationStringency, getSuppliedSamRefName(), "samConsensus", getMinQScore(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
				consensusSequence = consensusMap.get("samConsensus");
				tipAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
				targetRef = tipAlmtMember.targetReferenceFromMember();
				samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
			} else {
				targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
						ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefInfo.getSamRefName(), consensusSequence)));
				tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefInfo.getSamRefName()));
			}

			Alignment tipAlmt = tipAlmtMember.getAlignment();
			ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName());

			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getAcRefName(), getFeatureName()), false);

			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, samFileSession, consoleCmdContext, targetRef, consensusSequence);

			// translate segments to tip alignment reference
			List<QueryAlignedSegment> samRefToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
					tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
					samRefToTargetRefSegs);

			// translate segments to ancestor constraining reference
			List<QueryAlignedSegment> samRefToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, samRefToTipAlmtRefSegs, ancConstrainingRef);

			// trim down to the feature area.
			List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
					.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
			List<QueryAlignedSegment> samRefToAncConstrRefSegs = 
					ReferenceSegment.intersection(samRefToAncConstrRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

			SamRefSense samRefSense = getSamRefSense(samReporter);


			SamRecordFilter samRecordFilter;
			try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, samFileName, validationStringency)) {
				samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, samFileName, getSuppliedSamRefName());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Supplier<BaseNucleotideContext> contextSupplier = () -> {
				BaseNucleotideContext context = new BaseNucleotideContext();
				context.samRecordFilter = samRecordFilter;
				context.samRefInfo = samRefInfo;
				context.samRefSense = samRefSense;
				context.samRefToAncConstrRefSegs = QueryAlignedSegment.cloneList(samRefToAncConstrRefSegs);
				context.samReporter = samReporter;
				context.acRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
				for(QueryAlignedSegment samRefToAncConstrRefSeg: context.samRefToAncConstrRefSegs) {
					for(int samRefNt = samRefToAncConstrRefSeg.getQueryStart(); samRefNt <= samRefToAncConstrRefSeg.getQueryEnd(); samRefNt++) {
						int acRefNt = samRefNt+samRefToAncConstrRefSeg.getQueryToReferenceOffset();
						int resultSamRefNt = samRefNt;
						if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
							// we want to report results in the SAM file's own coordinates.
							resultSamRefNt = ReferenceSegment.reverseLocationSense(context.samRefInfo.getSamRefLength(), samRefNt);
						}
						context.acRefNtToInfo.put(acRefNt, new NucleotideReadCount(resultSamRefNt, acRefNt));
					}
				}
				return context;
			};
			BaseNucleotideResult mergedResult = SamUtils.pairedParallelSamIterate(contextSupplier, consoleCmdContext, samFileSession, validationStringency, this);
			
			List<NucleotideReadCount> nucleotideReadCounts = new ArrayList<NucleotideReadCount>(mergedResult.acRefNtToInfo.valueCollection());

			return formResult(nucleotideReadCounts, samReporter);
		}
	}

	
	
	private TIntObjectMap<BaseWithQuality> getAcRefNtToBaseWithQuality(BaseNucleotideContext context, SAMRecord samRecord) {
		TIntObjectMap<BaseWithQuality> acRefNtToBaseWithQuality = new TIntObjectHashMap<BaseWithQuality>();
		
		List<QueryAlignedSegment> readToSamRefSegs = context.samReporter.getReadToSamRefSegs(samRecord);
		String readString = samRecord.getReadString().toUpperCase();
		String qualityString = samRecord.getBaseQualityString();
		if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), context.samRefInfo.getSamRefLength());
			readString = FastaUtils.reverseComplement(readString);
			qualityString = StringUtils.reverseString(qualityString);
		}

		List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, context.samRefToAncConstrRefSegs);


		for(QueryAlignedSegment readToAncConstRefSeg: readToAncConstrRefSegs) {
			Integer queryStart = readToAncConstRefSeg.getQueryStart();
			Integer queryEnd = readToAncConstRefSeg.getQueryEnd();
			CharSequence readNts = SegmentUtils.base1SubString(readString, queryStart, queryEnd);
			CharSequence readQuality = SegmentUtils.base1SubString(qualityString, queryStart, queryEnd);
			Integer acRefNt = readToAncConstRefSeg.getRefStart();
			for(int i = 0; i < readNts.length(); i++) {
				char qualityChar = readQuality.charAt(i);
				int qScore = SamUtils.qualityCharToQScore(qualityChar);
				if(qScore < getMinQScore(context.samReporter)) {
					continue;
				}
				char readChar = readNts.charAt(i);
				acRefNtToBaseWithQuality.put(acRefNt+i, new BaseWithQuality(readChar, qScore));
			}
		}
		return acRefNtToBaseWithQuality;
	}
	
	private class BaseWithQuality {
		char base;
		int quality;
		public BaseWithQuality(char base, int quality) {
			super();
			this.base = base;
			this.quality = quality;
		}
	}
	
	
	
	@Override
	public void initContextForReader(BaseNucleotideContext context, SamReader reader) {
	}

	@Override
	public void processPair(BaseNucleotideContext context, SAMRecord record1, SAMRecord record2) {
		if(!context.samRecordFilter.recordPasses(record1)) {
    		processSingleton(context, record2);
		} else if(!context.samRecordFilter.recordPasses(record2)) {
    		processSingleton(context, record1);
    	} else {
    		TIntObjectMap<BaseWithQuality> acRefNtToBaseWithQuality1 = getAcRefNtToBaseWithQuality(context, record1);
    		TIntObjectMap<BaseWithQuality> acRefNtToBaseWithQuality2 = getAcRefNtToBaseWithQuality(context, record2);
    		int read1MapQ = record1.getMappingQuality();
    		int read2MapQ = record2.getMappingQuality();
			int readNameHashCoinFlip = Math.abs(record1.getReadName().hashCode()) % 2;

    		for(int acRefNt: acRefNtToBaseWithQuality1.keys()) {
    			BaseWithQuality baseWithQuality1 = acRefNtToBaseWithQuality1.get(acRefNt);
    			BaseWithQuality baseWithQuality2 = acRefNtToBaseWithQuality2.remove(acRefNt);
				NucleotideReadCount refNtInfo = context.acRefNtToInfo.get(acRefNt);
    			if(baseWithQuality2 == null) {
    				updateRefNtInfo(refNtInfo, baseWithQuality1.base);
    			} else {
    				int read1qual = baseWithQuality1.quality;
    				int read2qual = baseWithQuality2.quality;
    				if(read1qual < read2qual) {
        				updateRefNtInfo(refNtInfo, baseWithQuality2.base);
    				} else if(read1qual > read2qual) {
        				updateRefNtInfo(refNtInfo, baseWithQuality1.base);
    				} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ < read2MapQ) {
        				updateRefNtInfo(refNtInfo, baseWithQuality2.base);
    				} else if(read1MapQ != 255 && read2MapQ != 255 && read1MapQ > read2MapQ) {
        				updateRefNtInfo(refNtInfo, baseWithQuality1.base);
    				} else if(readNameHashCoinFlip == 0) {
        				updateRefNtInfo(refNtInfo, baseWithQuality1.base);
    				} else {
        				updateRefNtInfo(refNtInfo, baseWithQuality2.base);
    				}
    			}
    		}
    		for(int acRefNt: acRefNtToBaseWithQuality2.keys()) {
				NucleotideReadCount refNtInfo = context.acRefNtToInfo.get(acRefNt);
				updateRefNtInfo(refNtInfo, acRefNtToBaseWithQuality2.get(acRefNt).base);
    		}
    	}
	}

	@Override
	public void processSingleton(BaseNucleotideContext context, SAMRecord samRecord) {
		if(!context.samRecordFilter.recordPasses(samRecord)) {
			return;
		}
    	TIntObjectMap<BaseWithQuality> acRefNtToBaseWithQuality = getAcRefNtToBaseWithQuality(context, samRecord);
		for(int acRefNt: acRefNtToBaseWithQuality.keys()) {
			NucleotideReadCount refNtInfo = context.acRefNtToInfo.get(acRefNt);
			updateRefNtInfo(refNtInfo, acRefNtToBaseWithQuality.get(acRefNt).base);
		}
	}

	@Override
	public BaseNucleotideResult contextResult(BaseNucleotideContext context) {
		BaseNucleotideResult result = new BaseNucleotideResult();
		result.acRefNtToInfo = context.acRefNtToInfo;
		return result;
	}

	@Override
	public BaseNucleotideResult reduceResults(BaseNucleotideResult result1, BaseNucleotideResult result2) {
		BaseNucleotideResult mergedResult = new BaseNucleotideResult();
		mergedResult.acRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
		
		for(int key: result1.acRefNtToInfo.keys()) {
			NucleotideReadCount count1 = result1.acRefNtToInfo.get(key);
			NucleotideReadCount count2 = result2.acRefNtToInfo.get(key);
			
			NucleotideReadCount mergedCount = new NucleotideReadCount(count1.getSamRefNt(), count1.getAcRefNt());
			mergedCount.readsWithA = count1.readsWithA + count2.readsWithA;
			mergedCount.readsWithC = count1.readsWithC + count2.readsWithC;
			mergedCount.readsWithG = count1.readsWithG + count2.readsWithG;
			mergedCount.readsWithT = count1.readsWithT + count2.readsWithT;
			mergedCount.totalContributingReads = count1.totalContributingReads + count2.totalContributingReads;
			mergedResult.acRefNtToInfo.put(key, mergedCount);
		}
		return mergedResult;
	}




	public static class BaseNucleotideContext {
		SamReporter samReporter;
		SamRefInfo samRefInfo;
		List<QueryAlignedSegment> samRefToAncConstrRefSegs;
		SamRefSense samRefSense;
		SamRecordFilter samRecordFilter;
		TIntObjectMap<NucleotideReadCount> acRefNtToInfo;
	}

	public static class BaseNucleotideResult {
		TIntObjectMap<NucleotideReadCount> acRefNtToInfo;
	}

	protected abstract R formResult(List<NucleotideReadCount> nucleotideReadCounts, SamReporter samReporter);
	
	protected void updateRefNtInfo(NucleotideReadCount refNtInfo, char readChar) {
		refNtInfo.totalContributingReads++;
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}







	
}
