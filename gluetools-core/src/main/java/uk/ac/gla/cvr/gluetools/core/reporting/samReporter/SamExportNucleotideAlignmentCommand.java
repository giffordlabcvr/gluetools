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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import htsjdk.samtools.SamReader;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.NucleotideFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass(
		commandWords={"export", "nucleotide-alignment"}, 
		description = "Export part of the BAM file as a nucleotide FASTA alignment", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] ( -e <selectorName> | -r <relRefName> -f <featureName> [-c <lcStart> <lcEnd> | -o <ntStart> <ntEnd>] ) (-p | [-l] -t <targetRefName>) -a <linkingAlmtName> [-q <minQScore>] [-g <minMapQ>][-d <minDepth>] [-y <lineFeedStyle>] (-O <outputFileName> | -P)" },
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
						"-d <minDepth>, --minDepth <minDepth>                       Minimum depth",
						"-O <outputFileName>, --outputFileName <outputFileName>     Output file name",
						"-P, --preview                                              Preview",
						"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>        LF or CRLF",
				},
				furtherHelp = 
					"This command exports a FASTA nucleotide alignment from a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The summarized depths are based on a 'target' GLUE reference sequence. "+
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
					"The depth results will be limited to this feature location.\n"+
					"Reads will not contribute to the depth if their reported quality score at the relevant position is less than "+
					"<minQScore> (default value is derived from the module config). \n"+
					"No depth result will be generated for a nucleotide position if the number of contributing reads is less than <minDepth> "+
					"(default value is derived from the module config)",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamExportNucleotideAlignmentCommand extends SamBaseNucleotideCommand
	<CommandResult, SamExportNucleotideAlignmentCommandContext, SamExportNucleotideAlignmentInterimResult> 
	implements ProvidedProjectModeCommand{

	public static final String PREVIEW = "preview";
	public static final String OUTPUT_FILE_NAME = "outputFileName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	
	private Boolean preview;
	private String outputFileName;
	private LineFeedStyle lineFeedStyle;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		outputFileName = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if((outputFileName == null && !preview) || (outputFileName != null && preview)) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <outputFileName> or <preview> must be specified, but not both");
		}
	}

	@Override
	protected CommandResult formResult(
			CommandContext cmdContext, SamExportNucleotideAlignmentInterimResult mergedResult, SamReporter samReporter) {
		
		Map<String, DNASequence> fastaMap = mergedResult.getFastaMap();
		if(preview) {
			return new NucleotideFastaCommandResult(fastaMap);
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			consoleCmdContext.saveBytes(outputFileName, FastaUtils.mapToFasta(fastaMap, lineFeedStyle));
			return new OkResult();

		}
	}

	
	@Override
	public SamExportNucleotideAlignmentInterimResult contextResult(SamExportNucleotideAlignmentCommandContext context) {
		return new SamExportNucleotideAlignmentInterimResult(context.getFastaMap());
	}

	@Override
	public SamExportNucleotideAlignmentInterimResult reduceResults(SamExportNucleotideAlignmentInterimResult result1, 
			SamExportNucleotideAlignmentInterimResult result2) {
		Map<String, DNASequence> fastaMap = result1.getFastaMap();
		fastaMap.putAll(result2.getFastaMap());
		return new SamExportNucleotideAlignmentInterimResult(fastaMap);
	}


	@CompleterClass
	public static class Completer extends ReferenceLinkedSamReporterCommand.Completer {
		public Completer() {
			super();
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("outputFileName", false);
		}
	}

	@Override
	protected Supplier<SamExportNucleotideAlignmentCommandContext> getContextSupplier(SamRecordFilter samRecordFilter, 
			SamRefInfo samRefInfo, SamRefSense samRefSense, 
			List<QueryAlignedSegment> samRefToRelatedRefSegs, 
			List<ReferenceSegment> selectedRefSegs, SamReporter samReporter) {
		return () -> {
			SamExportNucleotideAlignmentCommandContext context = 
					new SamExportNucleotideAlignmentCommandContext(samReporter, samRefInfo, 
							QueryAlignedSegment.cloneList(samRefToRelatedRefSegs), samRefSense, selectedRefSegs, samRecordFilter);
			return context;
		};
	}


	@Override
	public void initContextForReader(SamExportNucleotideAlignmentCommandContext context, SamReader reader) {
	}


	@Override
	protected void processReadBase(SamExportNucleotideAlignmentCommandContext context, String readName, int relatedRefNt, char base) {
		context.processReadBase(readName, relatedRefNt, base);
	}

	
}
