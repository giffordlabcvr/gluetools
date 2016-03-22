package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"<alignmentName> [-r <acRefName> -f <featureName>] [-c] (-w <whereClause> | -a) (-o <fileName> | -p)"},
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining reference",
			"-f <featureName>, --featureName <featureName>  Restrict to a given feature",
			"-c, --recursive                                Include descendent members",
			"-o <fileName>, --fileName <fileName>           FASTA output file",
			"-p, --preview                                  Preview output", 
			"-w <whereClause>, --whereClause <whereClause>  Qualify exported members",
		    "-a, --allMembers                               Export all members"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export nucleotide alignment to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class FastaAlignmentExportCommand extends ModulePluginCommand<CommandResult, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaAlignmentExporter exporterPlugin) {
		return exporterPlugin.doExport((ConsoleCommandContext) cmdContext, 
				delegate.getFileName(), delegate.getAlignmentName(), delegate.getWhereClause(), delegate.getAcRefName(),
				delegate.getFeatureName(), delegate.getRecursive(), delegate.getPreview());
	}
	
	@CompleterClass
	public static class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {}
}