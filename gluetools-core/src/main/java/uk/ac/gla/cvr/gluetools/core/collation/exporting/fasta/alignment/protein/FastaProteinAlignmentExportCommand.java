package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"<alignmentName> -r <acRefName> -f <featureName> [-c] (-w <whereClause> | -a) [-d <orderStrategy>] (-o <fileName> | -p)"},
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining reference",
			"-f <featureName>, --featureName <featureName>        Protein-coding feature",
			"-c, --recursive                                      Include descendent members",
			"-o <fileName>, --fileName <fileName>                 FASTA output file",
			"-p, --preview                                        Preview output", 
			"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
		    "-a, --allMembers                                     Export all members",
		    "-d <orderStrategy>, --orderStrategy <orderStrategy>  Specify row ordering strategy"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export protein alignment to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class FastaProteinAlignmentExportCommand extends ModulePluginCommand<CommandResult, FastaProteinAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, true);
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaProteinAlignmentExporter exporterPlugin) {
		return exporterPlugin.doExport((ConsoleCommandContext) cmdContext, 
				delegate.getFileName(), delegate.getAlignmentName(), delegate.getWhereClause(), delegate.getAcRefName(),
				delegate.getFeatureName(), delegate.getRecursive(), delegate.getPreview(), delegate.getOrderStrategy());
	}
	
	@CompleterClass
	public static class Completer extends FastaAlignmentExportCommandDelegate.ExportCompleter {
		@Override
		protected boolean filterFeatureLocation(FeatureLocation fLoc) {
			return super.filterFeatureLocation(fLoc) &&
					fLoc.getFeature().codesAminoAcids();
		}
	}

}