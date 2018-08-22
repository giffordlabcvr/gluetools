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

public abstract class SamBaseNucleotideCommand<R extends CommandResult> extends ReferenceLinkedSamReporterCommand<R> 
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
			if(useMaxLikelihoodPlacer()) {
				Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, samFileName, samFileSession,
						validationStringency, getSuppliedSamRefName(), "samConsensus", getMinQScore(samReporter), getMinMapQ(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
				consensusSequence = consensusMap.get("samConsensus");
				AlignmentMember targetRefAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
				targetRef = targetRefAlmtMember.targetReferenceFromMember();
				samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
			} else {
				targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
						ReferenceSequence.pkMap(getTargetRefName()));
			}

			Alignment linkingAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, 
					Alignment.pkMap(getLinkingAlmtName()));
			ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelatedRefName());

			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRelatedRefName(), getFeatureName()), false);

			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, samFileSession, consoleCmdContext, targetRef, consensusSequence);

			AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());

			
			// translate segments to linking alignment coords
			List<QueryAlignedSegment> samRefToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
					linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
					samRefToTargetRefSegs);

			// translate segments to related reference
			List<QueryAlignedSegment> samRefToRelatedRefSegsFull = linkingAlmt.translateToRelatedRef(cmdContext, samRefToLinkingAlmtSegs, relatedRef);

			// trim down to the feature area.
			List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
					.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
			List<QueryAlignedSegment> samRefToRelatedRefSegs = 
					ReferenceSegment.intersection(samRefToRelatedRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

			SamRefSense samRefSense = getSamRefSense(samReporter);


			SamRecordFilter samRecordFilter;
			try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, samFileName, validationStringency)) {
				samRecordFilter = new SamUtils.ConjunctionBasedRecordFilter(
						new SamUtils.ReferenceBasedRecordFilter(samReader, samFileName, getSuppliedSamRefName()), 
						new SamUtils.MappingQualityRecordFilter(getMinMapQ(samReporter))
				);

				
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Supplier<BaseNucleotideContext> contextSupplier = () -> {
				BaseNucleotideContext context = new BaseNucleotideContext();
				context.samRecordFilter = samRecordFilter;
				context.samRefInfo = samRefInfo;
				context.samRefSense = samRefSense;
				context.samRefToRelatedRefSegs = QueryAlignedSegment.cloneList(samRefToRelatedRefSegs);
				context.samReporter = samReporter;
				context.relatedRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
				for(QueryAlignedSegment samRefToRelatedRefSeg: context.samRefToRelatedRefSegs) {
					for(int samRefNt = samRefToRelatedRefSeg.getQueryStart(); samRefNt <= samRefToRelatedRefSeg.getQueryEnd(); samRefNt++) {
						int relatedRefNt = samRefNt+samRefToRelatedRefSeg.getQueryToReferenceOffset();
						int resultSamRefNt = samRefNt;
						if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
							// we want to report results in the SAM file's own coordinates.
							resultSamRefNt = ReferenceSegment.reverseLocationSense(context.samRefInfo.getSamRefLength(), samRefNt);
						}
						context.relatedRefNtToInfo.put(relatedRefNt, new NucleotideReadCount(resultSamRefNt, relatedRefNt));
					}
				}
				return context;
			};
			BaseNucleotideResult mergedResult = SamUtils.pairedParallelSamIterate(contextSupplier, consoleCmdContext, samFileSession, validationStringency, this);
			
			List<NucleotideReadCount> nucleotideReadCounts = new ArrayList<NucleotideReadCount>(mergedResult.relatedRefNtToInfo.valueCollection());

			return formResult(nucleotideReadCounts, samReporter);
		}
	}

	
	
	private TIntObjectMap<BaseWithQuality> getRelatedRefNtToBaseWithQuality(BaseNucleotideContext context, SAMRecord samRecord) {
		TIntObjectMap<BaseWithQuality> relatedRefNtToBaseWithQuality = new TIntObjectHashMap<BaseWithQuality>();
		
		List<QueryAlignedSegment> readToSamRefSegs = context.samReporter.getReadToSamRefSegs(samRecord);
		String readString = samRecord.getReadString().toUpperCase();
		String qualityString = samRecord.getBaseQualityString();
		if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), context.samRefInfo.getSamRefLength());
			readString = FastaUtils.reverseComplement(readString);
			qualityString = StringUtils.reverseString(qualityString);
		}

		List<QueryAlignedSegment> readToRelatedRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, context.samRefToRelatedRefSegs);


		for(QueryAlignedSegment readToRelatedRefSeg: readToRelatedRefSegs) {
			Integer queryStart = readToRelatedRefSeg.getQueryStart();
			Integer queryEnd = readToRelatedRefSeg.getQueryEnd();
			CharSequence readNts = SegmentUtils.base1SubString(readString, queryStart, queryEnd);
			CharSequence readQuality = SegmentUtils.base1SubString(qualityString, queryStart, queryEnd);
			Integer relatedRefNt = readToRelatedRefSeg.getRefStart();
			for(int i = 0; i < readNts.length(); i++) {
				char qualityChar = readQuality.charAt(i);
				int qScore = SamUtils.qualityCharToQScore(qualityChar);
				if(qScore < getMinQScore(context.samReporter)) {
					continue;
				}
				char readChar = readNts.charAt(i);
				relatedRefNtToBaseWithQuality.put(relatedRefNt+i, new BaseWithQuality(readChar, qScore));
			}
		}
		return relatedRefNtToBaseWithQuality;
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
    		TIntObjectMap<BaseWithQuality> relatedRefNtToBaseWithQuality1 = getRelatedRefNtToBaseWithQuality(context, record1);
    		TIntObjectMap<BaseWithQuality> relatedRefNtToBaseWithQuality2 = getRelatedRefNtToBaseWithQuality(context, record2);
    		int read1MapQ = record1.getMappingQuality();
    		int read2MapQ = record2.getMappingQuality();
			int readNameHashCoinFlip = Math.abs(record1.getReadName().hashCode()) % 2;

    		for(int relatedRefNt: relatedRefNtToBaseWithQuality1.keys()) {
    			BaseWithQuality baseWithQuality1 = relatedRefNtToBaseWithQuality1.get(relatedRefNt);
    			BaseWithQuality baseWithQuality2 = relatedRefNtToBaseWithQuality2.remove(relatedRefNt);
				NucleotideReadCount refNtInfo = context.relatedRefNtToInfo.get(relatedRefNt);
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
    		for(int relatedRefNt: relatedRefNtToBaseWithQuality2.keys()) {
				NucleotideReadCount refNtInfo = context.relatedRefNtToInfo.get(relatedRefNt);
				updateRefNtInfo(refNtInfo, relatedRefNtToBaseWithQuality2.get(relatedRefNt).base);
    		}
    	}
	}

	@Override
	public void processSingleton(BaseNucleotideContext context, SAMRecord samRecord) {
		if(!context.samRecordFilter.recordPasses(samRecord)) {
			return;
		}
    	TIntObjectMap<BaseWithQuality> relatedRefNtToBaseWithQuality = getRelatedRefNtToBaseWithQuality(context, samRecord);
		for(int relatedRefNt: relatedRefNtToBaseWithQuality.keys()) {
			NucleotideReadCount refNtInfo = context.relatedRefNtToInfo.get(relatedRefNt);
			updateRefNtInfo(refNtInfo, relatedRefNtToBaseWithQuality.get(relatedRefNt).base);
		}
	}

	@Override
	public BaseNucleotideResult contextResult(BaseNucleotideContext context) {
		BaseNucleotideResult result = new BaseNucleotideResult();
		result.relatedRefNtToInfo = context.relatedRefNtToInfo;
		return result;
	}

	@Override
	public BaseNucleotideResult reduceResults(BaseNucleotideResult result1, BaseNucleotideResult result2) {
		BaseNucleotideResult mergedResult = new BaseNucleotideResult();
		mergedResult.relatedRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
		
		for(int key: result1.relatedRefNtToInfo.keys()) {
			NucleotideReadCount count1 = result1.relatedRefNtToInfo.get(key);
			NucleotideReadCount count2 = result2.relatedRefNtToInfo.get(key);
			
			NucleotideReadCount mergedCount = new NucleotideReadCount(count1.getSamRefNt(), count1.getRelatedRefNt());
			mergedCount.readsWithA = count1.readsWithA + count2.readsWithA;
			mergedCount.readsWithC = count1.readsWithC + count2.readsWithC;
			mergedCount.readsWithG = count1.readsWithG + count2.readsWithG;
			mergedCount.readsWithT = count1.readsWithT + count2.readsWithT;
			mergedCount.totalContributingReads = count1.totalContributingReads + count2.totalContributingReads;
			mergedResult.relatedRefNtToInfo.put(key, mergedCount);
		}
		return mergedResult;
	}




	public static class BaseNucleotideContext {
		SamReporter samReporter;
		SamRefInfo samRefInfo;
		List<QueryAlignedSegment> samRefToRelatedRefSegs;
		SamRefSense samRefSense;
		SamRecordFilter samRecordFilter;
		TIntObjectMap<NucleotideReadCount> relatedRefNtToInfo;
	}

	public static class BaseNucleotideResult {
		TIntObjectMap<NucleotideReadCount> relatedRefNtToInfo;
	}

	protected abstract R formResult(List<NucleotideReadCount> nucleotideReadCounts, SamReporter samReporter);
	
	protected void updateRefNtInfo(NucleotideReadCount refNtInfo, char readChar) {
		refNtInfo.totalContributingReads++;
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}







	
}
