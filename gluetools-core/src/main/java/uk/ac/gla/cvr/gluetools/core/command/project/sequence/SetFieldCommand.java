package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Collections;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"set", "field"}, 
	docoptUsages={"[-C] <fieldName> <fieldValue>"},
	docoptOptions={
			"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Set a field value for the sequence") 
public class SetFieldCommand extends SequenceModeCommand<UpdateResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";
	public static final String NO_COMMIT = "noCommit";
	
	private String fieldName;
	private String fieldValue;
	private Boolean noCommit;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		fieldValue = PluginUtils.configureStringProperty(configElem, FIELD_VALUE, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}


	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getSequenceMode(cmdContext).getProject();
		project.checkValidCustomSequenceFieldNames(Collections.singletonList(fieldName));
		Sequence sequence = lookupSequence(cmdContext);
		Object oldValue = sequence.readProperty(fieldName);
		Field field = project.getSequenceField(fieldName);
		Object newValue = field.getFieldType().getFieldTranslator().valueFromString(fieldValue);
		if(oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return new UpdateResult(Sequence.class, 0);
		}
		if(oldValue == null && newValue == null) {
			return new UpdateResult(Sequence.class, 0);
		}
		sequence.writeProperty(fieldName, newValue);
		if(!noCommit) {
			cmdContext.commit();
		}
		return new UpdateResult(Sequence.class, 1);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}

}
