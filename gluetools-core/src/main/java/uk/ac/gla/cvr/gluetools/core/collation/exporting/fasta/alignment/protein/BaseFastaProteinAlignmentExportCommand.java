package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

public abstract class BaseFastaProteinAlignmentExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaProteinAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, true);
	}
	
	protected String formAlmtString(CommandContext cmdContext,
			FastaProteinAlignmentExporter exporterPlugin) {
		String fastaString = exporterPlugin.exportAlignment(cmdContext, delegate.getAlignmentName(),
				delegate.getWhereClause(), (SimpleAlignmentColumnsSelector) delegate.getAlignmentColumnsSelector(cmdContext), delegate.getRecursive(),
				delegate.getOrderStrategy(), delegate.getExcludeEmptyRows(), delegate.getLineFeedStyle());
		return fastaString;
	}


}