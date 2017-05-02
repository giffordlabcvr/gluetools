package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"<alignmentName> [ -s <selectorName> | -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd> | -n <ntStart> <ntEnd>] ] [-c] (-w <whereClause> | -a) [-e] [-d <orderStrategy>] [-i [-m <minColUsage>]] [-y <lineFeedStyle>] (-o <fileName> | -p)"},
		docoptOptions={
			"-s <selectorName>, --selectorName <selectorName>      Column selector module",
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-l, --labelledCodon                                   Region between codon labels",
			"-n, --ntRegion                                        Specific nucleotide region",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify exported members",
		    "-a, --allMembers                                      Export all members",
		    "-e, --excludeEmptyRows                                Exclude empty rows",
		    "-d <orderStrategy>, --orderStrategy <orderStrategy>   Specify row ordering strategy",
		    "-i, --includeAllColumns                               Include columns for all NTs",
		    "-m <minColUsage>, --minColUsage <minColUsage>         Minimum included column usage",
			"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>   LF or CRLF",
			"-o <fileName>, --fileName <fileName>                  FASTA output file",
			"-p, --preview                                         Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export nucleotide alignment to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.\n"
				+ "The --labeledCodon option may be used only for coding features.\n" 
				+ "If --ntRegion is used, the coordinates are relative to the named reference sequence.") 
public class FastaAlignmentExportCommand extends BaseFastaAlignmentExportCommand<CommandResult> {

	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";

	public static final String INCLUDE_ALL_COLUMNS = "includeAllColumns";
	public static final String MIN_COLUMN_USAGE = "minColUsage";

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	private Boolean includeAllColumns;
	private Integer minColUsage;
	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
		this.includeAllColumns = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, INCLUDE_ALL_COLUMNS, false)).orElse(false);
		this.minColUsage = PluginUtils.configureIntProperty(configElem, MIN_COLUMN_USAGE, false);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
		if(this.minColUsage != null && !this.includeAllColumns) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <minColUsage> argument may only be used if <includeAllColumns> is specified");
		}

	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		String fastaAlmtString = formAlmtString(cmdContext, exporterPlugin);
		return exporterPlugin.formResult((ConsoleCommandContext) cmdContext, fastaAlmtString, fileName, preview);
	}
	
	@CompleterClass
	public static class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {}
}