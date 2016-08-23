package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"set", "field"}, 
	docoptUsages={"[-C] <fieldName> <fieldValue>"},
	docoptOptions={
			"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Set a field value for the current mode object") 
public class ConfigurableObjectSetFieldCommand extends Command<UpdateResult> {

	public static final String FIELD_NAME = PropertyCommandDelegate.FIELD_NAME;
	public static final String FIELD_VALUE = PropertyCommandDelegate.FIELD_VALUE;
	public static final String NO_COMMIT = PropertyCommandDelegate.NO_COMMIT;

	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureSetField(pluginConfigContext, configElem);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeSetField(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ModifiableFieldNameCompleter {}

}
