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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"<alignmentName> [-r <relRefName> -f <featureName>] [-c] (-w <whereClause> | -a) [-d <orderStrategy>] [-i [-m <minColUsage>]] (-o <fileName> | -p)"},
		docoptOptions={
			"-r <relRefName>, --relRefName <relRefName>            Related reference",
			"-f <featureName>, --featureName <featureName>         Restrict to a given feature",
			"-c, --recursive                                       Include descendent members",
			"-w <whereClause>, --whereClause <whereClause>         Qualify exported members",
		    "-a, --allMembers                                      Export all members",
		    "-i, --includeAllColumns                               Include columns for all NTs",
		    "-m <minColUsage>, --minColUsage <minColUsage>         Minimum included column usage",
		    "-d <orderStrategy>, --orderStrategy <orderStrategy>   Specify row ordering strategy",
			"-o <fileName>, --fileName <fileName>                  FASTA output file",
			"-p, --preview                                         Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export nucleotide alignment to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class FastaAlignmentExportCommand extends ModulePluginCommand<CommandResult, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

	public static final String INCLUDE_ALL_COLUMNS = "includeAllColumns";
	public static final String MIN_COLUMN_USAGE = "minColUsage";

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	private Boolean includeAllColumns;
	private Integer minColUsage;
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
		this.includeAllColumns = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, INCLUDE_ALL_COLUMNS, false)).orElse(false);
		this.minColUsage = PluginUtils.configureIntProperty(configElem, MIN_COLUMN_USAGE, false);
		if(this.minColUsage != null && !this.includeAllColumns) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <minColUsage> argument may only be used if <includeAllColumns> is specified");
		}
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		return exporterPlugin.doExport((ConsoleCommandContext) cmdContext, 
				delegate.getFileName(), delegate.getAlignmentName(), delegate.getWhereClause(), delegate.getAcRefName(),
				delegate.getFeatureName(), delegate.getRecursive(), delegate.getPreview(), includeAllColumns, 
				minColUsage, delegate.getOrderStrategy());
	}
	
	@CompleterClass
	public static class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {}
}