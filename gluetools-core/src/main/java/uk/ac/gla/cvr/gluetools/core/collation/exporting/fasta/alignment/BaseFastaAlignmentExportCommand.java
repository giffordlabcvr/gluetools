package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseFastaAlignmentExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaAlignmentExporter> implements ProvidedProjectModeCommand {

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
	
	protected String formAlmtString(CommandContext cmdContext,
			FastaAlignmentExporter exporterPlugin) {
		QueryMemberSupplier queryMemberSupplier = 
				new QueryMemberSupplier(delegate.getAlignmentName(), delegate.getRecursive(), delegate.getWhereClause());
		return FastaAlignmentExporter.exportAlignment(cmdContext, queryMemberSupplier, 
				delegate.getAlignmentColumnsSelector(cmdContext), 
				delegate.getOrderStrategy(), includeAllColumns, minColUsage, delegate.getExcludeEmptyRows(),  
				exporterPlugin.getIdTemplate(), delegate.getLineFeedStyle());
	}

	
}