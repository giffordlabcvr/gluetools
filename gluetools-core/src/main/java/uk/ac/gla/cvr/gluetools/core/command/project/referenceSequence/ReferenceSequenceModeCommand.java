package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class ReferenceSequenceModeCommand<R extends CommandResult> extends Command<R> {


	private String refSeqName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refSeqName = PluginUtils.configureStringProperty(configElem, "refSeqName", true);
	}

	protected String getRefSeqName() {
		return refSeqName;
	}

	@SuppressWarnings("rawtypes")
	public abstract static class FeatureNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				Expression exp = ExpressionFactory.matchExp(Feature.REF_SEQ_NAME_PATH, getRefSeqMode(cmdContext).getRefSeqName());
				return CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class, exp)).
						getColumnValues(Feature.NAME_PROPERTY);
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}

	protected static ReferenceSequenceMode getRefSeqMode(CommandContext cmdContext) {
		return (ReferenceSequenceMode) cmdContext.peekCommandMode();
	}
}
