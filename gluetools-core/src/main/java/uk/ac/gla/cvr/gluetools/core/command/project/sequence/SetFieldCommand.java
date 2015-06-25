package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"set", "field"}, 
	docoptUsages={"<fieldName> <fieldValue>"},
	description="Set a field value for the sequence") 
public class SetFieldCommand extends SequenceModeCommand {

	private String fieldName;
	private String fieldValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		fieldValue = PluginUtils.configureStringProperty(configElem, "fieldValue", true);
	}


	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Project project = getSequenceMode(cmdContext).getProject();
		Sequence sequence = lookupSequence(cmdContext);
		List<String> customFieldNames = project.getCustomSequenceFieldNames();
		if(!customFieldNames.contains(fieldName)) {
			throw new SequenceException(Code.INVALID_FIELD, fieldName, customFieldNames);
		}
		Field field = project.getSequenceField(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		sequence.writeProperty(fieldName, newValue);
		return CommandResult.OK;
	}




}
