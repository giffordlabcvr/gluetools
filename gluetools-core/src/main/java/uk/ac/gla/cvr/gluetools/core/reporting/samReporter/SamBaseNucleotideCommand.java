package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public abstract class SamBaseNucleotideCommand<R extends CommandResult> extends AlignmentTreeSamReporterCommand<R> implements ProvidedProjectModeCommand{


	@Override
	protected final R execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		
		DNASequence consensusSequence = null;
		ReferenceSequence targetRef;
		AlignmentMember tipAlmtMember;
		if(useMaxLikelihoodPlacer()) {
			Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
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

		List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, consoleCmdContext, targetRef, consensusSequence);
		
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

		final TIntObjectMap<NucleotideReadCount> acRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
		for(QueryAlignedSegment samRefToAncConstrRefSeg: samRefToAncConstrRefSegs) {
			for(int samRefNt = samRefToAncConstrRefSeg.getQueryStart(); samRefNt <= samRefToAncConstrRefSeg.getQueryEnd(); samRefNt++) {
				int acRefNt = samRefNt+samRefToAncConstrRefSeg.getQueryToReferenceOffset();
				int resultSamRefNt = samRefNt;
        		if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
        			// we want to report results in the SAM file's own coordinates.
        			resultSamRefNt = ReferenceSegment.reverseLocationSense(samRefInfo.getSamRefLength(), samRefNt);
        		}
				acRefNtToInfo.put(acRefNt, new NucleotideReadCount(resultSamRefNt, acRefNt));
			}
		}

        final RecordsCounter recordsCounter = samReporter.new RecordsCounter();
    	
        try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
    		SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());

        	SamUtils.iterateOverSamReader(samReader, samRecord -> {
        		if(!samRecordFilter.recordPasses(samRecord)) {
        			return;
        		}
        		
        		List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
				String readString = samRecord.getReadString().toUpperCase();
				String qualityString = samRecord.getBaseQualityString();
        		if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
        			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), samRefInfo.getSamRefLength());
        			readString = FastaUtils.reverseComplement(readString);
        			qualityString = StringUtils.reverseString(qualityString);
        		}
        		
        		
        		List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);


        		for(QueryAlignedSegment readToAncConstRefSeg: readToAncConstrRefSegs) {
        			Integer queryStart = readToAncConstRefSeg.getQueryStart();
					Integer queryEnd = readToAncConstRefSeg.getQueryEnd();
					CharSequence readNts = SegmentUtils.base1SubString(readString, queryStart, queryEnd);
					CharSequence readQuality = SegmentUtils.base1SubString(qualityString, queryStart, queryEnd);
        			Integer acRefNt = readToAncConstRefSeg.getRefStart();
        			for(int i = 0; i < readNts.length(); i++) {
						NucleotideReadCount refNtInfo = acRefNtToInfo.get(acRefNt+i);
						char qualityChar = readQuality.charAt(i);
						if(SamUtils.qualityCharToQScore(qualityChar) < getMinQScore(samReporter)) {
							continue;
						}
						char readChar = readNts.charAt(i);
						updateRefNtInfo(refNtInfo, readChar);
        			}
        		}
        		recordsCounter.processedRecord();
        		recordsCounter.logRecordsProcessed();
        	});
        	recordsCounter.logTotalRecordsProcessed();

        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
	
        List<NucleotideReadCount> nucleotideReadCounts = new ArrayList<NucleotideReadCount>(acRefNtToInfo.valueCollection());
        
        return formResult(nucleotideReadCounts, samReporter);
        
	}

	protected abstract R formResult(List<NucleotideReadCount> nucleotideReadCounts, SamReporter samReporter);
	
	protected void updateRefNtInfo(NucleotideReadCount refNtInfo, char readChar) {
		refNtInfo.totalContributingReads++;
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}







	
}
