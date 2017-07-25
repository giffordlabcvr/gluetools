package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class BaseExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaExporter> implements ProvidedProjectModeCommand {
	
	public static final String LINE_FEED_STYLE = "lineFeedStyle";

	private Expression whereClause;
	private Boolean allSequences;
	private LineFeedStyle lineFeedStyle;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
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

	protected LineFeedStyle getLineFeedStyle() {
		return lineFeedStyle;
	}

	private void usageError() {
		throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allSequences> must be specified, but not both");
	}

}