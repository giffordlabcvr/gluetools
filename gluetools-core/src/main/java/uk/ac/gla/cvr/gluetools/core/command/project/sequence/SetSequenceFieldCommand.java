package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="set-sequence-field")
@CommandClass(description="Set a field value for a sequence", 
	docoptUsages={"[-s <sourceName>] <sequenceID> -f <fieldName> <fieldValue>"},
	docoptOptions={
		"-s <sourceName>, --sourceName <sourceName>  Specify a particular source", 
		"-f <fieldName>, --fieldName <fieldName>  Name of the field"}) 
public class SetSequenceFieldCommand extends ProjectModeCommand {

	private String sourceName;
	private String sequenceID;
	private String fieldName;
	private String fieldValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", true);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		fieldValue = PluginUtils.configureStringProperty(configElem, "fieldValue", true);
	}


	// TODO sort out exceptions here.
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext, sourceName, sequenceID, false);
		List<String> customFieldNames = getCustomSequenceFieldNames(cmdContext);
		if(!customFieldNames.contains(fieldName)) {
			throw new SequenceException(Code.INVALID_FIELD, fieldName, customFieldNames);
		}
		Field field = getProjectMode(cmdContext).getSequenceField(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		sequence.writeProperty(fieldName, newValue);
		return CommandResult.OK;
	}


}
