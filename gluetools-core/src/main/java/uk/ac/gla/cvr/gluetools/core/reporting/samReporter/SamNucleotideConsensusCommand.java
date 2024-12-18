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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"nucleotide-consensus"}, 
		description = "Generate FASTA nucleotide consensus from a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] [-c <consensusID>] [-y <lineFeedStyle>] (-o <outputFileName> | -p)  [-q <minQScore>] [-g <minMapQ>] [-d <minDepth>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                    SAM/BAM input file",
						"-n <samRefSense>, --samRefSense <samRefSense>           SAM ref seq sense",
						"-s <samRefName>, --samRefName <samRefName>              Specific SAM ref seq",
						"-c <consensusID>, --consensusID <consensusID>           FASTA ID for consensus",
						"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>     LF or CRLF",
						"-o <outputFileName>, --outputFileName <outputFileName>  FASTA output file",
						"-p, --preview                                           Preview output on console",
						"-q <minQScore>, --minQScore <minQScore>                 Minimum Phred quality score",
						"-g <minMapQ>, --minMapQ <minMapQ>                       Minimum mapping quality score",
						"-d <minDepth>, --minDepth <minDepth>                    Minimum depth"
				},
				furtherHelp = 
					"This generates a consensus FASTA file from a SAM/BAM file. "+
					"If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
					"Reads will not contribute to the consensus if their reported quality score at the relevant position is less than "+
					"<minQScore> (default value is derived from the module config). \n"+
					"No consensus will be generated for a nucleotide position if the number of contributing reads is less than <minDepth> "+
					"(default value is derived from the module config).",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamNucleotideConsensusCommand extends ExtendedSamReporterCommand<CommandResult> implements ProvidedProjectModeCommand {

	public static final String CONSENSUS_ID = "consensusID";
	public static final String PREVIEW = "preview";
	public static final String OUTPUT_FILE_NAME = "outputFileName";
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	
	private String outputFileName;
	private boolean preview;
	private String consensusID;
	private LineFeedStyle lineFeedStyle;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.consensusID = PluginUtils.configureStringProperty(configElem, CONSENSUS_ID, "samConsensusSequence");
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		outputFileName = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE_NAME, false);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		if(outputFileName == null && !preview || outputFileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <outputFileName> or <preview> must be specified, but not both");
		}
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samFileName = getFileName();
		try(SamReporterPreprocessorSession samReporterPreprocessorSession = SamReporterPreprocessor.getPreprocessorSession(consoleCmdContext, samFileName, samReporter)) {
			DNASequence consensusSequence = samReporterPreprocessorSession.getConsensus(consoleCmdContext, samReporter, this, true);

			Map<String, DNASequence> samNtConsensusMap = new LinkedHashMap<String, DNASequence>();
			samNtConsensusMap.put(this.consensusID, consensusSequence);
			if(this.preview) {
				return new NucleotideFastaCommandResult(samNtConsensusMap);
			} else {
				byte[] fastaBytes = FastaUtils.mapToFasta(samNtConsensusMap, lineFeedStyle);
				consoleCmdContext.saveBytes(outputFileName, fastaBytes);
				return new OkResult();
			}
		}
	}
	
	public int getConsensusMinQScore(SamReporter samReporter) {
		return getSuppliedMinQScore().orElse(super.getConsensusMinQScore(samReporter));
	}

	public int getConsensusMinDepth(SamReporter samReporter) {
		return getSuppliedMinDepth().orElse(super.getConsensusMinDepth(samReporter));
	}

	public int getConsensusMinMapQ(SamReporter samReporter) {
		return getSuppliedMinMapQ().orElse(super.getConsensusMinMapQ(samReporter));
	}


	@CompleterClass
	public static class Completer extends ExtendedSamReporterCommand.Completer {
		public Completer() {
			super();
			registerPathLookup("outputFileName", false);
			registerEnumLookup("lineFieldStyle", LineFeedStyle.class);
		}
	}
}
