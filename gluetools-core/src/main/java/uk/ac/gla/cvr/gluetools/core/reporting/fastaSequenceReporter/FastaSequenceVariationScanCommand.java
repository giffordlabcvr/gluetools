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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.Map.Entry;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a FASTA file for variations", 
		docoptUsages = { "-i <fileName> -r <relRefName> -f <featureName> [-d] -t <targetRefName> -a <linkingAlmtName> [-w <whereClause>] [-e] [-c] [-v | -o]"+
		""},
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                       FASTA input file",
				"-r <relRefName>, --relRefName <relRefName>                 Related reference",
				"-f <featureName>, --featureName <featureName>              Feature to scan",
				"-d, --descendentFeatures                                   Include descendent features",
				"-t <targetRefName>, --targetRefName <targetRefName>        Target reference",
				"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
				"-w <whereClause>, --whereClause <whereClause>              Qualify variations",
				"-e, --excludeAbsent                                        Exclude absent variations",
				"-c, --excludeInsufficientCoverage                          Exclude where insufficient coverage",
				"-v, --showMatchesAsTable                                   Table with one row per match",
				"-o, --showMatchesAsDocument                                Document with one object per match",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "scans a section of the query sequence for variations based on an alignment between the target reference "+
		        "and the related reference, where the variations are defined. "+
				"The target reference sequence must be a member of the specified linking alignment."+
		        "The <relRefName> argument specifies a 'related' reference sequence. "+
				"If the linking alignment is constrained, the related reference must constrain an ancestor alignment "+
		        "of the linking alignment. Otherwise, it may be any reference sequence which shares membership of the "+
				"linking alignment with the target reference. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
				"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
				"The variation scan will be limited to the specified features. "+
				"If <whereClause> is used, this qualifies the set of variations which are scanned for "+
				"If --excludeAbsent is used, variations which were confirmed to be absent will not appear in the results. "+
				"If --excludeInsufficientCoverage is used, variations for which the query did not sufficiently cover the scanned "+
				"area will not appear in the results. "+
				"If --showMatchesAsTable is used, a table is returned with one row for each individual match. In this case the "+
				"selected variations must all be of the same type. "+
				"If --showMatchsAsDocument is used, a document is returned with an object for each individual match.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceVariationScanCommand extends FastaSequenceBaseVariationScanCommand {

	public static final String FILE_NAME = "fileName";

	private String fileName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaSequenceReporter fastaSequenceReporter) {

		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		Entry<String, DNASequence> fastaEntry = FastaSequenceReporter.getFastaEntry(consoleCmdContext, fileName);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		String targetRefName = getTargetRefName();
		
		return executeAux(cmdContext, fastaSequenceReporter, fastaID, fastaNTSeq, targetRefName, null);
	}


	@CompleterClass
	public static class Completer extends FastaSequenceReporterCommand.Completer {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}
	
}
