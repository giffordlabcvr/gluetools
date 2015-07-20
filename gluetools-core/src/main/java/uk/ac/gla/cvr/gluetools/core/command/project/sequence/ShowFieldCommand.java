package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.FieldCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"show", "field"}, 
	docoptUsages={"<fieldName>"},
	description="Show a field value for the sequence") 
public class ShowFieldCommand extends SequenceModeCommand {

	public static final String FIELD_NAME = "fieldName";
	
	private String fieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
	}


	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Project project = getSequenceMode(cmdContext).getProject();
		List<String> customFieldNames = project.getCustomSequenceFieldNames();
		Sequence sequence = lookupSequence(cmdContext);
		if(!customFieldNames.contains(fieldName)) {
			throw new SequenceException(Code.INVALID_FIELD, fieldName, customFieldNames);
		}
		Object value = sequence.readProperty(fieldName);
		if(value != null) {
			return new SimpleConsoleCommandResult(value.toString());
		} else {
			return new SimpleConsoleCommandResult("No value defined for field "+fieldName);
		}
	}

	@CompleterClass
	public static class Completer extends FieldCompleter {
	}


}
