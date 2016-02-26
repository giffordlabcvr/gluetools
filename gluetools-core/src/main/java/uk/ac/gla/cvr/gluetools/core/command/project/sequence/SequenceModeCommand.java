package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class SequenceModeCommand<R extends CommandResult> extends Command<R> {


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


	public static SequenceMode getSequenceMode(CommandContext cmdContext) {
		SequenceMode sequenceMode = (SequenceMode) cmdContext.peekCommandMode();
		return sequenceMode;
	}

	protected Sequence lookupSequence(CommandContext cmdContext) {
		Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class,
				Sequence.pkMap(getSourceName(), getSequenceID()));
		return sequence;
	}
	
	public abstract static class SequenceFieldNameCompleter extends AdvancedCmdCompleter {
		public SequenceFieldNameCompleter() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return 
							getSequenceMode(cmdContext).getProject().getCustomFieldNames(ConfigurableTable.SEQUENCE)
							.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
				}
			});
		}
	}

	
}
