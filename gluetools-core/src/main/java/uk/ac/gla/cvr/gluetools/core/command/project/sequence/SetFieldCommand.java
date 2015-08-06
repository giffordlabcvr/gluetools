package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.FieldCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"set", "field"}, 
	docoptUsages={"[-n] <fieldName> <fieldValue>"},
	docoptOptions={"-n, --noOverwrite  Do not overwrite an existing value"},
	description="Set a field value for the sequence", 
	furtherHelp=
		"If --noOverwrite is used, and the field already has a non-null value, no update will take place. "+
		"By default the command will overwrite any existing value.") 
public class SetFieldCommand extends SequenceModeCommand<OkResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";
	public static final String NO_OVERWRITE = "noOverwrite";
	
	private String fieldName;
	private String fieldValue;
	private Optional<Boolean> noOverwrite;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
		noOverwrite = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, NO_OVERWRITE, false));
	}


	@Override
	public OkResult execute(CommandContext cmdContext) {
		Project project = getSequenceMode(cmdContext).getProject();
		List<String> customFieldNames = project.getCustomSequenceFieldNames();
		Sequence sequence = lookupSequence(cmdContext);
		if(!customFieldNames.contains(fieldName)) {
			throw new SequenceException(Code.INVALID_FIELD, fieldName, customFieldNames);
		}
		Field field = project.getSequenceField(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		if(noOverwrite.orElse(false)) {
			if(sequence.readProperty(fieldName) != null) {
				return CommandResult.OK;
			}
		}
		sequence.writeProperty(fieldName, newValue);
		cmdContext.commit();
		return CommandResult.OK;
	}

	@SuppressWarnings("rawtypes")
	@CompleterClass
	public static class Completer extends FieldCompleter {
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			List<String> suggestions = new ArrayList<String>();
			if(argStrings.size() == 0) {
				suggestions.add("-n");
				suggestions.add("--noOverwrite");
				suggestions.addAll(getCustomFieldNames(cmdContext));
			} else if(argStrings.size() == 1) {
				String arg0 = argStrings.get(0);
				if(arg0.equals("-n") || arg0.equals("--noOverwrite")) {
					suggestions.addAll(getCustomFieldNames(cmdContext));
				}
			}
			return suggestions;
		}
		
	}


}
