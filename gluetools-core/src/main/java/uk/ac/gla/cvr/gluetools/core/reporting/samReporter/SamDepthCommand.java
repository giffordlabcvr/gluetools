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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;

@CommandClass(
		commandWords={"depth"}, 
		description = "Summarise depth in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] -r <acRefName> -f <featureName> (-p | [-l] [-t <targetRefName>] [-a <tipAlmtName>]) [-q <minQScore>] [-g <minMapQ>][-d <minDepth>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
						"-n <samRefSense>, --samRefSense <samRefSense>        SAM ref seq sense",
						"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
						"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
						"-f <featureName>, --featureName <featureName>        Feature",
						"-p, --maxLikelihoodPlacer                            Use ML placer module",
						"-l, --autoAlign                                      Auto-align consensus",
						"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
						"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
						"-q <minQScore>, --minQScore <minQScore>              Minimum Phred quality score",
						"-g <minMapQ>, --minMapQ <minMapQ>                    Minimum mapping quality score",
						"-d <minDepth>, --minDepth <minDepth>                 Minimum depth"
				},
				furtherHelp = 
					"This command summarises read depth in a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The summarized depths are based on a 'target' GLUE reference sequence's place in the alignment tree. "+
					"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
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
					"The depth results will be limited to this feature location.\n"+
					"Reads will not contribute to the depth if their reported quality score at the relevant position is less than "+
					"<minQScore> (default value is derived from the module config). \n"+
					"No depth result will be generated for a nucleotide position if the number of contributing reads is less than <minDepth> "+
					"(default value is derived from the module config)",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamDepthCommand extends SamBaseNucleotideCommand<SamDepthResult> implements ProvidedProjectModeCommand{


	@Override
	protected SamDepthResult formResult(
			List<NucleotideReadCount> nucleotideReadCounts, SamReporter samReporter) {
        int minDepth = getMinDepth(samReporter);
		nucleotideReadCounts = nucleotideReadCounts.stream()
        		.filter(nrc -> nrc.totalContributingReads >= minDepth)
        		.collect(Collectors.toList());
        
        Comparator<NucleotideReadCount> comparator = new Comparator<NucleotideReadCount>() {
			@Override
			public int compare(NucleotideReadCount nrc1, NucleotideReadCount nrc2) {
				return Integer.compare(nrc1.getAcRefNt(), nrc2.getAcRefNt());
			}};
		Collections.sort(nucleotideReadCounts, comparator);
 		return new SamDepthResult(nucleotideReadCounts);
	}

	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {
		public Completer() {
			super();
			registerEnumLookup("samRefSense", SamRefSense.class);
		}
	}
	
}
