package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

public abstract class BaseFastaAlignmentExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

	private FastaAlignmentExportCommandDelegate delegate = new FastaAlignmentExportCommandDelegate();
	
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		delegate.configure(pluginConfigContext, configElem, false);
	}

	protected FastaAlignmentExportCommandDelegate getDelegate() {
		return delegate;
	}

	
}