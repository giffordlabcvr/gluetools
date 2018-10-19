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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import gnu.trove.map.hash.TIntObjectHashMap;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"nucleotide"}, 
		description = "Summarise nucleotides in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] ( -e <selectorName> | -r <relRefName> -f <featureName> [-c <lcStart> <lcEnd> | -o <ntStart> <ntEnd>] ) (-p | [-l] -t <targetRefName>) -a <linkingAlmtName> [-q <minQScore>] [-g <minMapQ>] [-d <minDepth>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                       SAM/BAM input file",
						"-n <samRefSense>, --samRefSense <samRefSense>              SAM ref seq sense",
						"-s <samRefName>, --samRefName <samRefName>                 Specific SAM ref seq",
						"-e <selectorName>, --selectorName <selectorName>           Column selector module",
						"-r <relRefName>, --relRefName <relRefName>                 Related reference sequence",
						"-f <featureName>, --featureName <featureName>              Feature",
						"-c, --labelledCodon                                        Region between codon labels",
						"-o, --ntRegion                                             Specific nucleotide region",
						"-p, --maxLikelihoodPlacer                                  Use ML placer module",
						"-l, --autoAlign                                            Auto-align consensus",
						"-t <targetRefName>, --targetRefName <targetRefName>        Target GLUE reference",
						"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
						"-q <minQScore>, --minQScore <minQScore>                    Minimum Phred quality score",
						"-g <minMapQ>, --minMapQ <minMapQ>                          Minimum mapping quality score",
						"-d <minDepth>, --minDepth <minDepth>                       Minimum depth"
				},
				furtherHelp = 
					"This command summarises nucleotides in a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The summarized locations are based on a 'target' GLUE reference sequence. "+
					"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
					"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
					"identified as the closest according to this placement. "+
					"Otherwise, the target reference must be specified using <targetRefName>."+
					"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
					"is the reference sequence  mentioned in the SAM file. "+
					"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
					"consensus and the target GLUE reference. \n"+
					"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
					"The target reference sequence must be a member of the "+
					"'linking alignment', specified by <linkingAlmtName>. "+
			        "The <relRefName> argument specifies the related reference sequence, on which the feature is defined. "+
					"If the linking alignment is constrained, the related reference must constrain an ancestor alignment "+
			        "of the linking alignment. Otherwise, it may be any reference sequence which shares membership of the "+
					"linking alignment with the target reference. "+
					"The <featureName> arguments specifies a feature location on the related reference. "+
					"The nucleotide summary will be limited to this feature location.\n"+
					"Reads will not contribute to the summary if their reported quality score at the relevant position is less than "+
					"<minQScore> (default value is derived from the module config). \n"+
					"No summary will be generated for a nucleotide position if the number of contributing reads is less than <minDepth> "+
					"(default value is derived from the module config)",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamNucleotideCommand extends SamBaseNucleotideCommand
	<SamNucleotideResult, SamNucleotideCommandContext, SamNucleotideCommandInterimResult>
	implements ProvidedProjectModeCommand{


	@Override
	protected void processReadBase(SamNucleotideCommandContext context, String readName, int relatedRefNt, char base) {
		SamNucleotideResidueCount residueCount = context.getRelatedRefNtToInfo().get(relatedRefNt);
		if(base == 'A') {
			residueCount.incrementReadsWithA();
		} else if(base == 'C') {
			residueCount.incrementReadsWithC();
		} else if(base == 'G') {
			residueCount.incrementReadsWithG();
		} else if(base == 'T') {
			residueCount.incrementReadsWithT();
		}
	}

	@Override
	protected SamNucleotideResult formResult(
			CommandContext cmdContext, SamNucleotideCommandInterimResult mergedResult, SamReporter samReporter) {
		List<SamNucleotideResidueCount> nucleotideReadCounts = new ArrayList<SamNucleotideResidueCount>(mergedResult.getRelatedRefNtToInfo().valueCollection());

		int minDepth = getMinDepth(samReporter);
		nucleotideReadCounts = nucleotideReadCounts.stream()
        		.filter(nrc -> nrc.getTotalContributingReads() >= minDepth)
        		.collect(Collectors.toList());
        
        Comparator<SamNucleotideResidueCount> comparator = new Comparator<SamNucleotideResidueCount>() {
			@Override
			public int compare(SamNucleotideResidueCount nrc1, SamNucleotideResidueCount nrc2) {
				return Integer.compare(nrc1.getRelatedRefNt(), nrc2.getRelatedRefNt());
			}};
		Collections.sort(nucleotideReadCounts, comparator);
 		return new SamNucleotideResult(nucleotideReadCounts);
	}

	@Override
	public SamNucleotideCommandInterimResult contextResult(SamNucleotideCommandContext context) {
		return new SamNucleotideCommandInterimResult(context.getRelatedRefNtToInfo());
	}

	@Override
	public SamNucleotideCommandInterimResult reduceResults(SamNucleotideCommandInterimResult result1, SamNucleotideCommandInterimResult result2) {
		SamNucleotideCommandInterimResult mergedResult = new SamNucleotideCommandInterimResult(new TIntObjectHashMap<SamNucleotideResidueCount>());
		
		for(int key: result1.getRelatedRefNtToInfo().keys()) {
			SamNucleotideResidueCount count1 = result1.getRelatedRefNtToInfo().get(key);
			SamNucleotideResidueCount count2 = result2.getRelatedRefNtToInfo().get(key);
			
			SamNucleotideResidueCount mergedCount = new SamNucleotideResidueCount(count1.getSamRefNt(), count1.getRelatedRefNt());
			mergedCount.setReadsWithA(count1.getReadsWithA() + count2.getReadsWithA());
			mergedCount.setReadsWithC(count1.getReadsWithC() + count2.getReadsWithC());
			mergedCount.setReadsWithG(count1.getReadsWithG() + count2.getReadsWithG());
			mergedCount.setReadsWithT(count1.getReadsWithT() + count2.getReadsWithT());
			mergedResult.getRelatedRefNtToInfo().put(key, mergedCount);
		}
		return mergedResult;
	}

	@Override
	protected Supplier<SamNucleotideCommandContext> getContextSupplier(SamRecordFilter samRecordFilter, SamRefInfo samRefInfo, SamRefSense samRefSense, List<QueryAlignedSegment> samRefToRelatedRefSegs, List<ReferenceSegment> selectedRefSegs, SamReporter samReporter) {
		return () -> {
			SamNucleotideCommandContext context = new SamNucleotideCommandContext(samReporter, samRefInfo, QueryAlignedSegment.cloneList(samRefToRelatedRefSegs), samRefSense, samRecordFilter);
			for(QueryAlignedSegment samRefToRelatedRefSeg: context.getSamRefToRelatedRefSegs()) {
				for(int samRefNt = samRefToRelatedRefSeg.getQueryStart(); samRefNt <= samRefToRelatedRefSeg.getQueryEnd(); samRefNt++) {
					int relatedRefNt = samRefNt+samRefToRelatedRefSeg.getQueryToReferenceOffset();
					int resultSamRefNt = samRefNt;
					if(context.getSamRefSense().equals(SamRefSense.REVERSE_COMPLEMENT)) {
						// we want to report results in the SAM file's own coordinates.
						resultSamRefNt = ReferenceSegment.reverseLocationSense(context.getSamRefInfo().getSamRefLength(), samRefNt);
					}
					context.getRelatedRefNtToInfo().put(relatedRefNt, new SamNucleotideResidueCount(resultSamRefNt, relatedRefNt));
				}
			}
			return context;
		};
	}

	@CompleterClass
	public static class Completer extends ReferenceLinkedSamReporterCommand.Completer {
		public Completer() {
			super();
		}
	}

}
