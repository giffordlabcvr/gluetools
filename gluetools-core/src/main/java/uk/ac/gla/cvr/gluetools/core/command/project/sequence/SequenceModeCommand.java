package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class SequenceModeCommand extends Command {


	private String sourceName;
	private String sequenceID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
	}

	protected String getSourceName() {
		return sourceName;
	}

	protected String getSequenceID() {
		return sequenceID;
	}


	protected SequenceMode getSequenceMode(CommandContext cmdContext) {
		SequenceMode sequenceMode = (SequenceMode) cmdContext.peekCommandMode();
		return sequenceMode;
	}

	protected Sequence lookupSequence(CommandContext cmdContext) {
		Sequence sequence = GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class,
				Sequence.pkMap(getSourceName(), getSequenceID()));
		return sequence;
	}

	public static class FieldCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			List<String> suggestions = new ArrayList<String>();
			if(argStrings.size() == 0) {
				suggestions.addAll(getCustomFieldNames(cmdContext));
			}
			return suggestions;
		}

		protected List<String> getCustomFieldNames(ConsoleCommandContext cmdContext) {
			SequenceMode sequenceMode = (SequenceMode) cmdContext.peekCommandMode();
			Project project = sequenceMode.getProject();
			List<String> customFieldNames = project.getCustomSequenceFieldNames();
			return customFieldNames;
		}
		
	}
	
	
}
