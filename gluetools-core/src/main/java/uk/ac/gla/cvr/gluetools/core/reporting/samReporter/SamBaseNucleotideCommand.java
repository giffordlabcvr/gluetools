package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
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
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

@CommandClass(
		commandWords={"nucleotide"}, 
		description = "Summarise nucleotides in a SAM/BAM file", 
				docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> -f <featureName> (-p | [-l] [-t <targetRefName>] [-a <tipAlmtName>]) [-q <minQScore>] [-d <minDepth>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
						"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
						"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
						"-f <featureName>, --featureName <featureName>        Feature",
						"-p, --maxLikelihoodPlacer                            Use ML placer module",
						"-l, --autoAlign                                      Auto-align consensus",
						"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
						"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
						"-q <minQScore>, --minQScore <minQScore>              Minimum Phred quality score",
						"-d <minDepth>, --minDepth <minDepth>                 Minimum depth"
				},
				furtherHelp = 
					"This command summarises nucleotides in a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The summarized locations are based on a 'target' GLUE reference sequence's place in the alignment tree. "+
					"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
					"identified as the closest according to this placement. "+
					"The target reference may alternatively be specified using <targetRefName>."+
					"Or, inferred from the SAM reference name, if <targetRefName> is not supplied and the module is appropriately configured. "+
					"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
					"is the reference sequence  mentioned in the SAM file. "+
					"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
					"consensus and the target GLUE reference. \n"+
					"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
					"The target reference sequence must be a member of a constrained "+
					"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
					"inferred from the target reference if possible. "+
					"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
					"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
					"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
					"The nucleotide summary will be limited to this feature location.\n"+
					"Reads will not contribute to the summary if their reported quality score at the relevant position is less than "+
					"<minQScore> (default value is derived from the module config). \n"+
					"No summary will be generated for a nucleotide position if the number of contributing reads is less than <minDepth> "+
					"(default value is derived from the module config)",
		metaTags = {CmdMeta.consoleOnly}	
)
public abstract class SamBaseNucleotideCommand<R extends CommandResult> extends AlignmentTreeSamReporterCommand<R> implements ProvidedProjectModeCommand{


	@Override
	protected final R execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samRefName;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		DNASequence consensusSequence = null;
		ReferenceSequence targetRef;
		AlignmentMember tipAlmtMember;
		if(useMaxLikelihoodPlacer()) {
			Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinDepth(samReporter));
			consensusSequence = consensusMap.get("samConsensus");
			tipAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
			targetRef = tipAlmtMember.targetReferenceFromMember();
			samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
		} else {
			targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefName, consensusSequence)));
			tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefName));
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

		
		final TIntObjectMap<NucleotideReadCount> acRefNtToInfo = new TIntObjectHashMap<NucleotideReadCount>();
		for(QueryAlignedSegment samRefToAncConstrRefSeg: samRefToAncConstrRefSegs) {
			for(int samRefNt = samRefToAncConstrRefSeg.getQueryStart(); samRefNt <= samRefToAncConstrRefSeg.getQueryEnd(); samRefNt++) {
				int acRefNt = samRefNt+samRefToAncConstrRefSeg.getQueryToReferenceOffset();
				acRefNtToInfo.put(acRefNt, new NucleotideReadCount(samRefNt, acRefNt));
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
        		List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);


        		final String readString = samRecord.getReadString().toUpperCase();
        		final String qualityString = samRecord.getBaseQualityString();

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
