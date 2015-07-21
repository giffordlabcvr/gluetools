package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.LinkedList;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AlignmentModeCommand extends Command {


	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	protected String getAlignmentName() {
		return alignmentName;
	}

	public abstract static class FeatureNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				AlignmentMode alignmentMode = (AlignmentMode) cmdContext.peekCommandMode();
				Expression exp = ExpressionFactory.matchExp(Feature.ALIGNMENT_NAME_PATH, alignmentMode.getAlignmentName());
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class, exp)).
						getColumnValues(Feature.NAME_PROPERTY));
			}
			return suggestions;
		}
	}

	protected AlignmentMode getAlignmentMode(CommandContext cmdContext) {
		return (AlignmentMode) cmdContext.peekCommandMode();
	}
}
