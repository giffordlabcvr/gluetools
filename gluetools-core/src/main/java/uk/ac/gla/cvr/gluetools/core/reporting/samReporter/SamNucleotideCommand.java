package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
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
				docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> -f <featureName> [-l] [-t <targetRefName>] [-a <tipAlmtName>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
						"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
						"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
						"-f <featureName>, --featureName <featureName>        Feature to translate",
						"-l, --autoAlign                                      Auto-align consensus",
						"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
						"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				},
				furtherHelp = 
					"This command summarises nucleotides in a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The summarized locations are based on a 'target' GLUE reference sequence's place in the alignment tree. "+
					"If <targetRefName> is not supplied, it may be inferred from the SAM reference name, if the module is appropriately configured. "+
					"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
					"is the reference sequence  mentioned in the SAM file. "+
					"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
					"consensus and the target GLUE reference. \n"+
					"The target reference sequence must be a member of a constrained "+
					"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
					"inferred from the target reference if possible. "+
					"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
					"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
					"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
		    " The nucleotide summary will be limited to this feature location.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamNucleotideCommand extends AlignmentTreeSamReporterCommand<SamNucleotideResult> implements ProvidedProjectModeCommand{


	@Override
	protected SamNucleotideResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samRefName;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		DNASequence consensusSequence = null;
		if(useMaxLikelihoodPlacer()) {
			consensusSequence = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus").get("samConsensus");
		}
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
				ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefName, consensusSequence)));
		AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefName));
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

        		for(QueryAlignedSegment readToAncConstRefSeg: readToAncConstrRefSegs) {
        			CharSequence nts = SegmentUtils.base1SubString(readString, readToAncConstRefSeg.getQueryStart(), readToAncConstRefSeg.getQueryEnd());
        			Integer acRefNt = readToAncConstRefSeg.getRefStart();
        			for(int i = 0; i < nts.length(); i++) {
						NucleotideReadCount refNtInfo = acRefNtToInfo.get(acRefNt+i);
						char readChar = nts.charAt(i);
	        			if(readChar == 'A') {
	        				refNtInfo.readsWithA++;
	        			} else if(readChar == 'C') {
	        				refNtInfo.readsWithC++;
	        			} else if(readChar == 'G') {
	        				refNtInfo.readsWithG++;
	        			} else if(readChar == 'T') {
	        				refNtInfo.readsWithT++;
	        			}
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
        Comparator<NucleotideReadCount> comparator = new Comparator<NucleotideReadCount>() {
			@Override
			public int compare(NucleotideReadCount nrc1, NucleotideReadCount nrc2) {
				return Integer.compare(nrc1.getAcRefNt(), nrc2.getAcRefNt());
			}};
		Collections.sort(nucleotideReadCounts, comparator);
 		return new SamNucleotideResult(nucleotideReadCounts);
		
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}







	
}
