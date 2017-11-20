package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a FASTA file for variations", 
		docCategory = "Type-specific module commands",
		docoptUsages = { "-i <fileName> -r <acRefName> [-m] -f <featureName> [-d] [-t <targetRefName>] [-a <tipAlmtName>] [-w <whereClause>] [-e] [-l [-v [-n] [-o]]]"+
		""},
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
				"-f <featureName>, --featureName <featureName>        Feature to scan",
				"-d, --descendentFeatures                             Include descendent features",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations",
				"-e, --excludeAbsent                                  Exclude absent variations",
				"-l, --showPatternLocsSeparately                      Add row per pattern location",
				"-v, --showMatchValuesSeparately                      Add row per match value",
				"-n, --showMatchNtLocations                           Add match NT start/end columns",
				"-o, --showMatchLcLocations                           Add codon start/end columns"
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "scans a section of the query "+
				"If <targetRefName> is not supplied, it may be inferred from the FASTA sequence ID, if the module is appropriately configured. "+
				"sequence for variations based on the target reference sequence's "+
				"place in the alignment tree. The target reference sequence must be a member of a constrained "+
		        "'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
		        "inferred from the target reference if possible. "+
		        "The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
				"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
				"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
				"path between the target reference and the ancestor-constraining reference, in the alignment tree. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
				"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
				"The variation scan will be limited to the specified features. "+
				"If <whereClause> is used, this qualifies the set of variations which are scanned for "+
				"If --excludeAbsent is used, variations which were confirmed to be absent will not appear in the results.",
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
	protected FastaSequenceVariationScanResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {

		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		Entry<String, DNASequence> fastaEntry = FastaSequenceReporter.getFastaEntry(consoleCmdContext, fileName);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		String targetRefName = getTargetRefName();
		
		if(targetRefName == null) {
			targetRefName = fastaSequenceReporter.targetRefNameFromFastaId(consoleCmdContext, fastaID);
		}

		return executeAux(cmdContext, fastaSequenceReporter, fastaID,
				fastaNTSeq, targetRefName);
	}


	@CompleterClass
	public static class Completer extends FastaSequenceReporterCommand.Completer {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}
	
}
