package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Collections;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"unset", "field"}, 
	docoptUsages={"[-C] <fieldName>"},
	docoptOptions={
		"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Unset a field value for the sequence", 
	furtherHelp="After the command has executed, the sequence will have no value for the specified field.") 
public class UnsetFieldCommand extends SequenceModeCommand<UpdateResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String NO_COMMIT = "noCommit";
	
	private String fieldName;
	private Boolean noCommit;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
	}


	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		getSequenceMode(cmdContext).getProject()
		.checkCustomFieldNames(ConfigurableTable.sequence, Collections.singletonList(fieldName));
		Sequence sequence = lookupSequence(cmdContext);
		Object oldValue = sequence.readProperty(fieldName);
		if(oldValue != null) {
			sequence.writeProperty(fieldName, null);
		}
		if(!noCommit) {
			cmdContext.commit();
		}
		return new UpdateResult(Sequence.class, oldValue == null ? 0 : 1);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}


}
