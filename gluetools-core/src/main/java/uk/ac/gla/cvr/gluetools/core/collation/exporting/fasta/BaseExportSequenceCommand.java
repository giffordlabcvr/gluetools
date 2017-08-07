package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseExportSequenceCommand<R extends CommandResult> extends BaseExportCommand<R> implements ProvidedProjectModeCommand {
	
	private Expression whereClause;
	private Boolean allSequences;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		allSequences = PluginUtils.configureBooleanProperty(configElem, "allSequences", true);
		if(whereClause == null && !allSequences) {
			usageError();
		}
		if(whereClause != null && allSequences) {
			usageError();
		}
	}

	protected Expression getWhereClause() {
		return whereClause;
	}

	private void usageError() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allSequences> must be specified, but not both");
	}

	
}