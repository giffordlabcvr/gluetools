package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"unset", "field"}, 
	docoptUsages={"[-C] <fieldName>"},
	docoptOptions={
		"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Unset a field value for the variation-alignment note", 
	furtherHelp="After the command has executed, the feature will have no value for the specified field.") 
public class VarAlmtNoteUnsetFieldCommand extends VarAlmtNoteModeCommand<UpdateResult> {

	public static final String FIELD_NAME = PropertyCommandDelegate.FIELD_NAME;
	public static final String NO_COMMIT = PropertyCommandDelegate.NO_COMMIT;
	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureUnsetField(pluginConfigContext, configElem);
	}


	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeUnsetField(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ModifiableFieldNameCompleter {}


}
