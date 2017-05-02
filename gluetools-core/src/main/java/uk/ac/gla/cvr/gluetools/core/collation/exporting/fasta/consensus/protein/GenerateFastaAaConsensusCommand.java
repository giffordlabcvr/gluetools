package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.protein;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"generate", "fasta", "aa-consensus"}, 
		docoptUsages={"<alignmentName> -r <relRefName> -f <featureName> [-l <lcStart> <lcEnd>] [-c] (-w <whereClause> | -a) [-i <consensusID>] [-y <lineFeedStyle>] (-o <fileName> | -p)"},
		docoptOptions={
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-l, --labelledCodon                                   Region between codon labels",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify included members",
		    "-a, --allMembers                                      Include all members",
			"-i <consensusID>, --consensusID <consensusID>         FASTA ID for consensus",
			"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>   LF or CRLF",
			"-o <fileName>, --fileName <fileName>                  FASTA output file",
			"-p, --preview                                         Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Generate an amino-acid consensus sequence as FASTA", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.\n"
				+ "The --labeledCodon option may be used only for coding features.\n" 
				+ "If --ntRegion is used, the coordinates are relative to the named reference sequence.") 
public class GenerateFastaAaConsensusCommand extends ModulePluginCommand<CommandResult, AminoAcidConsensusGenerator> implements ProvidedProjectModeCommand {

	public static final String CONSENSUS_ID = "consensusID";
	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	private String consensusID;
	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
		this.consensusID = PluginUtils.configureStringProperty(configElem, CONSENSUS_ID, "consensusSequence");
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, AminoAcidConsensusGenerator generatorPlugin) {
		return generatorPlugin.doGenerate((ConsoleCommandContext) cmdContext, 
				fileName, delegate.getAlignmentName(), delegate.getWhereClause(), 
				(SimpleAlignmentColumnsSelector) delegate.getAlignmentColumnsSelector(cmdContext), delegate.getRecursive(), preview,
				consensusID, delegate.getLineFeedStyle());
	}
	
	@CompleterClass
	public static class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {}
}