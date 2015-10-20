package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"set", "field"}, 
	docoptUsages={"[-o] [-f] <fieldName> <fieldValue>"},
	docoptOptions={
			"-o, --overwrite    Overwrite non-null existing value [default: false]", 
			"-f, --forceUpdate  Force update, includes setting null [default: false]", 
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Set a field value for the sequence", 
	furtherHelp=
		"Unless --overwrite is used, if the field already has a non-null value, no update will take place. "+
		"By default the command will overwrite any existing value.") 
public class SetFieldCommand extends SequenceModeCommand<UpdateResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";
	public static final String OVERWRITE = "overwrite";
	public static final String FORCE_UPDATE = "forceUpdate";
	
	private String fieldName;
	private String fieldValue;
	private Boolean overwrite;
	private Boolean forceUpdate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
		overwrite = PluginUtils.configureBooleanProperty(configElem, OVERWRITE, true);
		forceUpdate = PluginUtils.configureBooleanProperty(configElem, FORCE_UPDATE, true);
	}


	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getSequenceMode(cmdContext).getProject();
		List<String> customFieldNames = project.getCustomSequenceFieldNames();
		Sequence sequence = lookupSequence(cmdContext);
		if(!customFieldNames.contains(fieldName)) {
			throw new SequenceException(Code.INVALID_FIELD, fieldName, customFieldNames);
		}
		Field field = project.getSequenceField(fieldName);
		Object oldValue = sequence.readProperty(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		if(oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return new UpdateResult(Sequence.class, 0);
		}
		if(oldValue == null && newValue == null) {
			return new UpdateResult(Sequence.class, 0);
		}
		if(oldValue != null && !overwrite) {
			return new UpdateResult(Sequence.class, 0);
		}
		if(newValue == null && !forceUpdate) {
			return new UpdateResult(Sequence.class, 0);
		}
		sequence.writeProperty(fieldName, newValue);
		cmdContext.commit();
		return new UpdateResult(Sequence.class, 1);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}

}
